package com.rarible.protocol.union.enrichment.service

import com.mongodb.client.result.DeleteResult
import com.rarible.core.apm.SpanType
import com.rarible.core.apm.withSpan
import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.loadMetaSynchronously
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.OriginService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.enrichment.converter.EnrichedItemConverter
import com.rarible.protocol.union.enrichment.meta.content.ContentMetaService
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.util.spent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException

@Component
class EnrichmentItemService(
    private val itemServiceRouter: BlockchainRouter<ItemService>,
    private val itemRepository: ItemRepository,
    private val enrichmentOrderService: EnrichmentOrderService,
    private val enrichmentAuctionService: EnrichmentAuctionService,
    private val itemMetaService: ItemMetaService,
    private val contentMetaService: ContentMetaService,
    private val originService: OriginService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun get(itemId: ShortItemId): ShortItem? {
        return itemRepository.get(itemId)
    }

    suspend fun getItemCollection(itemId: ShortItemId): CollectionIdDto? {
        val collectionId = itemServiceRouter.getService(itemId.blockchain)
            .getItemCollectionId(itemId.itemId) ?: return null
        return CollectionIdDto(itemId.blockchain, collectionId)
    }

    suspend fun getItemOrigins(itemId: ShortItemId): List<String> {
        val collectionId = getItemCollection(itemId)
        return originService.getOrigins(collectionId)
    }

    suspend fun getOrEmpty(itemId: ShortItemId): ShortItem {
        return itemRepository.get(itemId) ?: ShortItem.empty(itemId)
    }

    suspend fun save(item: ShortItem): ShortItem {
        return itemRepository.save(item.withCalculatedFields())
    }

    suspend fun delete(itemId: ShortItemId): DeleteResult? {
        val now = nowMillis()
        val result = itemRepository.delete(itemId)
        logger.info("Deleting Item [{}], deleted: {} ({}ms)", itemId.toDto().fullId(), result?.deletedCount, spent(now))
        return result
    }

    suspend fun findAll(ids: List<ShortItemId>): List<ShortItem> {
        return itemRepository.getAll(ids)
    }

    suspend fun fetch(itemId: ShortItemId): UnionItem {
        val now = nowMillis()
        val itemDto = itemServiceRouter.getService(itemId.blockchain).getItemById(itemId.itemId)
        logger.info("Fetched item [{}] ({} ms)", itemId.toDto().fullId(), spent(now))
        return itemDto
    }

    suspend fun fetchOrNull(itemId: ShortItemId): UnionItem? {
        return try {
            fetch(itemId)
        } catch (e: WebClientResponseException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                null
            } else {
                throw e
            }
        }
    }

    // [orders] is a set of already fetched orders that can be used as cache to avoid unnecessary 'getById' calls
    suspend fun enrichItem(
        shortItem: ShortItem?,
        item: UnionItem? = null,
        orders: Map<OrderIdDto, OrderDto> = emptyMap(),
        auctions: Map<AuctionIdDto, AuctionDto> = emptyMap(),
        meta: Map<ItemIdDto, UnionMeta> = emptyMap(),
        syncMetaDownload: Boolean = false,
        metaPipeline: String = "default" // TODO PT-49
    ) = coroutineScope {

        require(shortItem != null || item != null)
        val itemId = shortItem?.id?.toDto() ?: item!!.id

        val fetchedItem = async { item ?: fetch(ShortItemId(itemId)) }

        val metaHint = meta[itemId]
        val itemMeta = if (metaHint != null) {
            CompletableDeferred(metaHint)
        } else {
            val sync = (syncMetaDownload || item?.loadMetaSynchronously == true)
            withSpanAsync("fetchMeta", spanType = SpanType.CACHE) {
                itemMetaService.get(itemId, sync, metaPipeline)
            }
        }
        val bestOrders = enrichmentOrderService.fetchMissingOrders(
            existing = shortItem?.getAllBestOrders() ?: emptyList(),
            orders = orders
        )

        val auctionIds = shortItem?.auctions ?: emptySet()

        val auctionsData = async { enrichmentAuctionService.fetchAuctionsIfAbsent(auctionIds, auctions) }

        val itemDto = EnrichedItemConverter.convert(
            item = fetchedItem.await(),
            shortItem = shortItem,
            // replacing inner IPFS urls with public urls
            meta = contentMetaService.exposePublicUrls(itemMeta.await(), itemId),
            orders = bestOrders,
            auctions = auctionsData.await()
        )
        logger.info("Enriched item {}: {}", itemId.fullId(), itemDto)
        itemDto
    }

    private fun <T> CoroutineScope.withSpanAsync(
        spanName: String,
        spanType: String = SpanType.APP,
        block: suspend () -> T
    ): Deferred<T> = async { withSpan(name = spanName, type = spanType, body = block) }

}
