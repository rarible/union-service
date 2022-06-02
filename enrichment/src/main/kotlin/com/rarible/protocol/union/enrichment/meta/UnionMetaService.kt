package com.rarible.protocol.union.enrichment.meta

import com.rarible.core.apm.SpanType
import com.rarible.core.apm.withSpan
import com.rarible.loader.cache.CacheLoaderService
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Component

@Component
class UnionMetaService(
    @Qualifier("union.meta.cache.loader.service")
    private val unionMetaCacheLoaderService: CacheLoaderService<UnionMeta>,
    private val unionMetaMetrics: UnionMetaMetrics,
    private val unionMetaLoader: UnionMetaLoader
) {

    private val logger = LoggerFactory.getLogger(UnionMetaService::class.java)

    companion object {
        private val SVG_TAG = "<svg"
    }

    /**
     * Return available meta or `null` if it hasn't been loaded, has failed, or hasn't been requested yet.
     * For missed meta no scheduling operations will be performed
     */
    suspend fun getAvailableMeta(itemIds: List<ItemIdDto>): Map<ItemIdDto, UnionMeta> {
        val keyMap = itemIds.associateBy { it.fullId() }
        val result = HashMap<ItemIdDto, UnionMeta>()
        val cached = withSpan(name = "fetchCachedMeta", type = SpanType.CACHE) {
            unionMetaCacheLoaderService.getAll(keyMap.keys.toList())
        }
        cached.forEach {
            val id = keyMap[it.key]!!
            unionMetaMetrics.onMetaCacheHitOrMiss(
                itemId = id,
                hitOrMiss = it.isMetaInitiallyLoadedOrFailed()
            )
            val meta = it.getAvailable()
            if (meta != null) {
                result[id] = meta
            }
        }
        return result
    }

    /**
     * Return available meta or `null` if it hasn't been loaded, has failed, or hasn't been requested yet.
     * Schedule an update in the last case.
     */
    suspend fun getAvailableMetaOrScheduleLoading(itemId: ItemIdDto): UnionMeta? =
        getAvailableMetaOrLoadSynchronously(itemId, false)

    /**
     * Return available meta, if any. Otherwise, load the meta in the current coroutine (it may be slow).
     * Additionally, schedule loading if the meta hasn't been requested for this item.
     */
    suspend fun getAvailableMetaOrLoadSynchronously(
        itemId: ItemIdDto,
        synchronous: Boolean
    ): UnionMeta? {
        val metaCacheEntry = unionMetaCacheLoaderService.get(itemId.fullId())
        var availableMeta = metaCacheEntry.getAvailable()
        var metaShouldBeRefreshed = false
        unionMetaMetrics.onMetaCacheHitOrMiss(
            itemId = itemId,
            hitOrMiss = metaCacheEntry.isMetaInitiallyLoadedOrFailed()
        )
        if (availableMeta != null) {
            //TODO workaround for BRAVO-1954:svg in url
            if (!removeCachedMetaWithSvgInUrl(availableMeta, itemId)) {
                return availableMeta
            } else {
                metaShouldBeRefreshed = true
            }
        }
        if (!metaShouldBeRefreshed && metaCacheEntry.isMetaInitiallyLoadedOrFailed()) {
            logger.info("Meta loading for item ${itemId.fullId()} was failed")
            return null
        }
        if (!synchronous && (!metaCacheEntry.isMetaInitiallyScheduledForLoading() || metaShouldBeRefreshed)) {
            scheduleLoading(itemId)
        }
        if (synchronous) {
            logger.info("Loading meta synchronously for ${itemId.fullId()}")
            return loadMetaSynchronously(itemId)
        }
        return null
    }

    suspend fun loadMetaSynchronously(itemId: ItemIdDto): UnionMeta? {
        val itemMeta = try {
            unionMetaLoader.load(itemId)
        } catch (e: Exception) {
            logger.warn("Synchronous meta loading failed for ${itemId.fullId()}")
            // Schedule meta loading. Firstly, with retry purpose. Secondly, to cache the "error" if it persists.
            unionMetaCacheLoaderService.update(itemId.fullId())
            null
        }
        if (itemMeta != null) {
            logger.warn("Saving synchronously loaded meta to cache for ${itemId.fullId()}")
            try {
                unionMetaCacheLoaderService.save(itemId.fullId(), itemMeta)
            } catch (e: Exception) {
                if (e !is OptimisticLockingFailureException && e !is DuplicateKeyException) {
                    logger.error("Failed to save synchronously loaded meta to cache for ${itemId.fullId()}")
                    throw e
                }
            }
        }
        return itemMeta
    }

    private suspend fun removeCachedMetaWithSvgInUrl (
        availableMeta: UnionMeta?,
        itemId: ItemIdDto
    ) : Boolean {
        var isRemoved = false
        availableMeta?.content?.forEach { url ->
            if (SVG_TAG in url.url) {
                logger.info("Removing from cache svg Item with id: ${itemId.fullId()}")
                unionMetaCacheLoaderService.remove(itemId.fullId())
                isRemoved = true
            }
        }
        return isRemoved
    }

    /**
     * Save pre-defined meta for an item. Useful in tests.
     */
    suspend fun save(itemId: ItemIdDto, unionMeta: UnionMeta) {
        unionMetaCacheLoaderService.save(itemId.fullId(), unionMeta)
    }

    /**
     * Schedule an update (or initial loading) of metadata.
     */
    suspend fun scheduleLoading(itemId: ItemIdDto) {
        logger.info("Scheduling meta update for {}", itemId.fullId())
        unionMetaCacheLoaderService.update(itemId.fullId())
    }
}
