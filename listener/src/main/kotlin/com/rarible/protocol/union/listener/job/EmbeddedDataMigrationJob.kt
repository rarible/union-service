package com.rarible.protocol.union.listener.job

import com.rarible.core.meta.resource.model.EmbeddedContent
import com.rarible.core.meta.resource.parser.ipfs.IpfsUrlResourceParser
import com.rarible.loader.cache.internal.CacheRepository
import com.rarible.loader.cache.internal.MongoCacheEntry
import com.rarible.protocol.union.api.client.DefaultUnionWebClientCustomizer
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.producer.UnionInternalBlockchainEventProducer
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.UnionContentMetaService
import com.rarible.protocol.union.enrichment.meta.UnionMetaCacheLoader
import com.rarible.protocol.union.enrichment.meta.cache.IpfsContentCache
import com.rarible.protocol.union.enrichment.meta.embedded.EmbeddedContentService
import com.rarible.protocol.union.enrichment.meta.embedded.LegacyEmbeddedContentUrlDetector
import com.rarible.protocol.union.enrichment.meta.embedded.UnionEmbeddedContent
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.util.concurrent.atomic.AtomicInteger

@Component
@Deprecated("Should be removed after migration")
class EmbeddedDataMigrationJob(
    private val legacyEmbeddedContentUrlDetector: LegacyEmbeddedContentUrlDetector,
    private val embeddedContentService: EmbeddedContentService,
    private val unionContentMetaService: UnionContentMetaService,
    private val cacheRepository: CacheRepository,
    private val eventProducer: UnionInternalBlockchainEventProducer,
    private val enrichmentItemService: EnrichmentItemService,
    private val ipfsContentCache: IpfsContentCache,
    @Autowired(required = false)
    dataProvider: DataProvider?
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val batchSize = 1000
    private val asyncChuckSize = 16

    private val dataProvider = dataProvider ?: WebClientDataProvider(createWebClient(), logger)

    private val ipfsParser = IpfsUrlResourceParser()

    private val cUpdated = AtomicInteger()
    private val cMigrated = AtomicInteger()
    private val cFailed = AtomicInteger()
    private val cSkipped = AtomicInteger()
    private val cIpfsUrlUpdated = AtomicInteger()
    private val cIpfsCacheUpdated = AtomicInteger()

    private val counters = mapOf(
        "updated" to cUpdated,
        "migrated" to cMigrated,
        "skipped" to cSkipped,
        "failed" to cFailed,
        "ipfs_url_updated" to cIpfsUrlUpdated,
        "ipfs_cache_updated" to cIpfsCacheUpdated,
    )

    fun migrate(continuation: String?): Flow<String> {
        return flow {
            logger.info("Starting to migrate Meta, state: $continuation")
            var next = continuation
            do {
                next = migrateBatch(next)
                if (next != null) {
                    emit(next)
                }
            } while (next != null)
        }
    }

    private suspend fun migrateBatch(fromId: String?): String? {
        counters.values.forEach { it.set(0) }
        val entries = cacheRepository.findAll<UnionMeta>(UnionMetaCacheLoader.TYPE, fromId, batchSize)
        coroutineScope {
            entries.chunked(asyncChuckSize).map { chunk ->
                chunk.map {
                    async {
                        tryMigrateMeta(it)
                    }
                }.awaitAll()
            }
        }
        val continuation = entries.lastOrNull()?.key
        logger.info("Checked ${entries.size}, state: $continuation, counters: $counters")
        return continuation
    }

    private suspend fun tryMigrateMeta(entry: MongoCacheEntry<UnionMeta>) {
        try {
            try {
                migrateMeta(entry)
                // And one more try in case of concurrency, originally, should not happen
            } catch (ex: OptimisticLockingFailureException) {
                migrateMeta(cacheRepository.get(UnionMetaCacheLoader.TYPE, entry.key)!!)
            } catch (ex: DuplicateKeyException) {
                migrateMeta(cacheRepository.get(UnionMetaCacheLoader.TYPE, entry.key)!!)
            }
        } catch (e: Exception) {
            // Some kind of unexpected exception, should not happen
            logger.warn("Failed to migrate meta for Item ${entry.key}", e)
            cFailed.incrementAndGet()
        }
    }

    private suspend fun migrateMeta(entry: MongoCacheEntry<UnionMeta>) {
        val data = entry.data

        var contentUpdated = false
        var contentMigrated = false

        val updatedContent = data.content.map {
            if (it.url.startsWith("https://storage.opensea.io")) {
                logger.info("Found legacy OpenSea content URL for Item: ${entry.key}")
            }
            val embedded = unionContentMetaService.detectEmbeddedContent(it.url)
            if (embedded != null) {
                // There is embedded content instead of URL (somebody sent it to us previously)
                contentUpdated = true
                contentMigrated = true
                embedContent(it, embedded)
            } else if (legacyEmbeddedContentUrlDetector.isLegacyEmbeddedContentUrl(it.url)) {
                logger.info("Detect legacy embedded content: ${entry.key}")
                // Legacy embedded content urls (from flow or eth), data should be copied to Union
                contentUpdated = true
                contentMigrated = true
                migrateEmbeddedContent(it, entry.key)
            } else if (isLegacyIpfsUrl(it.url)) {
                // Full IPFS url which should be abstract
                contentUpdated = true
                updateIpfsUrl(it)
            } else {
                it
            }
        }

        // Filling IPFS cache
        updatedContent.forEach { content ->
            content.properties?.let { properties ->
                if (properties.isFull()) {
                    ipfsParser.parse(content.url)?.let {
                        cIpfsCacheUpdated.incrementAndGet()
                        ipfsContentCache.save(it, properties)
                    }
                }
            }
        }

        // Nothing changed, abort
        if (!contentUpdated) {
            cSkipped.incrementAndGet()
            return
        }

        val updatedMeta = cacheRepository.save(
            type = UnionMetaCacheLoader.TYPE,
            key = entry.key,
            data = entry.data.copy(content = updatedContent),
            cachedAt = entry.cachedAt
        )
        logger.info("Updated meta for Item ${entry.key}")

        // Only internal data like IPFS url has been changed, we don't need to notify market
        if (!contentMigrated) {
            cUpdated.incrementAndGet()
            return
        }

        notify(updatedMeta)

        cMigrated.incrementAndGet()
    }

    private suspend fun embedContent(
        original: UnionMetaContent,
        embedded: EmbeddedContent
    ): UnionMetaContent {
        val contentMeta = embedded.meta

        val properties = unionContentMetaService.convertToProperties(contentMeta)
            ?: original.properties
            ?: UnionImageProperties() // The same logic as for remote meta - we can't determine type, image by default

        val toSave = UnionEmbeddedContent(
            id = unionContentMetaService.getEmbeddedId(embedded.content),
            mimeType = properties.mimeType ?: embedded.meta.mimeType,
            size = embedded.content.size,
            data = embedded.content
        )

        embeddedContentService.save(toSave)

        return original.copy(
            url = unionContentMetaService.getEmbeddedSchemaUrl(toSave.id),
            properties = properties
        )
    }

    private suspend fun migrateEmbeddedContent(content: UnionMetaContent, itemId: String): UnionMetaContent {
        val entity = dataProvider.getData(content.url, itemId) ?: return content

        val mimeType = content.properties?.mimeType
            ?: entity.headers.getFirst(HttpHeaders.CONTENT_TYPE) // should be NOT null

        val data = entity.body

        if (data == null || data.isEmpty()) {
            logger.warn("Can't migrate embedded content for $itemId - failed to fetch data")
            return content
        }
        if (mimeType == null) {
            logger.warn("Can't migrate embedded content for $itemId - failed to resolve mimeType")
            return content
        }

        val embedded = UnionEmbeddedContent(
            id = unionContentMetaService.getEmbeddedId(data),
            mimeType = mimeType,
            size = data.size,
            data = data
        )

        embeddedContentService.save(embedded)

        val url = unionContentMetaService.getEmbeddedSchemaUrl(embedded.id)
        return content.copy(url = url)
    }

    private fun isLegacyIpfsUrl(url: String): Boolean {
        return url.startsWith("https://rarible.mypinata.cloud/")
    }

    private suspend fun updateIpfsUrl(content: UnionMetaContent): UnionMetaContent {
        val ipfsUrl = ipfsParser.parse(content.url)
        return if (ipfsUrl != null) { // Originally, it should NOT be null
            cIpfsUrlUpdated.incrementAndGet()
            content.copy(url = ipfsUrl.toSchemaUrl())
        } else {
            content
        }
    }

    private suspend fun notify(entry: MongoCacheEntry<UnionMeta>) {
        val itemId = IdParser.parseItemId(entry.key)
        val item = enrichmentItemService.fetchOrNull(ShortItemId(itemId))
        if (item == null) {
            logger.info("Embedded meta migration for item ${entry.key} finished, but Item NOT_FOUND")
            return
        }
        val itemWithMeta = item.copy(meta = entry.data)
        val message = KafkaEventFactory.internalItemEvent(UnionItemUpdateEvent(itemWithMeta))
        eventProducer.getProducer(itemId.blockchain).send(message)
        logger.info("Embedded meta migration for item ${entry.key} finished, notification sent")
    }

    private fun createWebClient(): WebClient {
        val builder = WebClient.builder()
        DefaultUnionWebClientCustomizer().customize(builder)
        return builder.build()
    }
}

interface DataProvider {

    suspend fun getData(url: String, itemId: String): HttpData?
}

class HttpData(
    val body: ByteArray?,
    val headers: HttpHeaders
)

class WebClientDataProvider(
    private val webClient: WebClient,
    private val logger: Logger
) : DataProvider {

    override suspend fun getData(url: String, itemId: String): HttpData? {
        val entity = try {
            webClient.get()
                .uri(url)
                .retrieve()
                .toEntity(ByteArray::class.java)
                .awaitFirst()
        } catch (e: Exception) {
            logger.warn("Can't migrate embedded content for $itemId - failed to fetch data: ${e.message}")
            return null
        }

        return HttpData(
            headers = entity.headers,
            body = entity.body
        )
    }

}