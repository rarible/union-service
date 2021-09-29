package com.rarible.protocol.union.enrichment.service.event

import com.rarible.core.common.nowMillis
import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.event.ItemEventDelete
import com.rarible.protocol.union.enrichment.event.ItemEventListener
import com.rarible.protocol.union.enrichment.event.ItemEventUpdate
import com.rarible.protocol.union.enrichment.model.ItemSellStats
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.BestOrderService
import com.rarible.protocol.union.enrichment.service.ItemService
import com.rarible.protocol.union.enrichment.service.OwnershipService
import com.rarible.protocol.union.enrichment.util.spent
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ItemEventService(
    private val itemService: ItemService,
    private val ownershipService: OwnershipService,
    private val itemEventListeners: List<ItemEventListener>,
    private val bestOrderService: BestOrderService
) {

    private val logger = LoggerFactory.getLogger(ItemEventService::class.java)

    // If ownership was updated, we need to recalculate totalStock/sellers for related item,
    // also, we can specify here Order which triggered this update - ItemService
    // can use this full Order to avoid unnecessary getOrderById calls
    suspend fun onOwnershipUpdated(ownershipId: ShortOwnershipId, order: OrderDto?) {
        val itemId = ShortItemId(ownershipId.blockchain, ownershipId.token, ownershipId.tokenId)
        optimisticLock {
            val item = itemService.get(itemId)
            if (item == null) {
                logger.debug(
                    "Item [{}] not found in DB, skipping sell stats update on Ownership event: [{}]",
                    itemId, ownershipId
                )
            } else {
                val refreshedSellStats = ownershipService.getItemSellStats(itemId)
                val currentSellStats = ItemSellStats(item.sellers, item.totalStock)
                if (refreshedSellStats != currentSellStats) {
                    val updatedItem = item.copy(
                        sellers = refreshedSellStats.sellers,
                        totalStock = refreshedSellStats.totalStock
                    )
                    logger.info(
                        "Updating Item [{}] with new sell stats, was [{}] , now: [{}]",
                        itemId, currentSellStats, refreshedSellStats
                    )
                    val saved = itemService.save(updatedItem)
                    notifyUpdate(saved, null, order)
                } else {
                    logger.debug(
                        "Sell stats of Item [{}] are the same as before Ownership event [{}], skipping update",
                        itemId, ownershipId
                    )
                }
            }
        }
    }

    suspend fun onItemUpdated(item: ItemDto) {
        val received = ShortItemConverter.convert(item)
        val existing = itemService.getOrEmpty(received.id)
        notifyUpdate(existing, item)
    }

    suspend fun onItemBestSellOrderUpdated(itemId: ShortItemId, order: OrderDto) {
        updateOrder(itemId, order) { item ->
            item.copy(bestSellOrder = bestOrderService.getBestSellOrder(item, order))
        }
    }

    suspend fun onItemBestBidOrderUpdated(itemId: ShortItemId, order: OrderDto) {
        updateOrder(itemId, order) { item ->
            item.copy(bestBidOrder = bestOrderService.getBestBidOrder(item, order))
        }
    }

    private suspend fun updateOrder(
        itemId: ShortItemId,
        order: OrderDto,
        orderUpdateAction: suspend (item: ShortItem) -> ShortItem
    ) = coroutineScope {
        optimisticLock {
            val current = itemService.get(itemId)
            val exist = current != null
            val short = current ?: ShortItem.empty(itemId)

            val updated = orderUpdateAction(short)

            if (short != updated) {
                if (updated.isNotEmpty()) {
                    val saved = itemService.save(updated)
                    notifyUpdate(saved, null, order)
                } else if (exist) {
                    itemService.delete(itemId)
                    logger.info("Deleted Item [{}] without enrichment data", itemId)
                    notifyUpdate(updated, null, order)
                }
            } else {
                logger.info("Item [{}] not changed after order updated, event won't be published", itemId)
            }
        }
    }

    private suspend fun updateItem(existing: ShortItem, updated: ShortItem): ShortItem {
        val now = nowMillis()
        val result = itemService.save(updated.copy(version = existing.version))
        logger.info("Updated Item [{}]: {} ({}ms)", updated.id, updated, spent(now))
        return result
    }

    suspend fun onItemDeleted(itemId: ShortItemId) {
        val deleted = deleteItem(itemId)
        notifyDelete(itemId)
        if (deleted) {
            logger.info("Item [{}] deleted (removed from NFT-Indexer)", itemId)
        }
    }

    private suspend fun deleteItem(itemId: ShortItemId): Boolean {
        val result = itemService.delete(itemId)
        return result != null && result.deletedCount > 0
    }

    suspend fun onLockCreated(itemId: ShortItemId) {
        logger.info("Updating Item [{}] marked as Unlockable", itemId)
        val shortItem = itemService.getOrEmpty(itemId)
        val updated = shortItem.copy(unlockable = true)
        val saved = itemService.save(updated)
        notifyUpdate(saved)
    }

    private suspend fun notifyDelete(itemId: ShortItemId) {
        val event = ItemEventDelete(itemId.toDto())
        itemEventListeners.forEach { it.onEvent(event) }
    }

    // Potentially we could have updated Order here (no matter - bid/sell) and when we need to fetch
    // full version of the order, we can use this already fetched Order if it has same ID (hash)
    private suspend fun notifyUpdate(
        short: ShortItem,
        item: ItemDto? = null,
        order: OrderDto? = null
    ) = coroutineScope {
        val dto = itemService.enrichItem(short, item, order)
        val event = ItemEventUpdate(dto)
        itemEventListeners.forEach { it.onEvent(event) }
    }
}
