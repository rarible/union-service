package com.rarible.protocol.union.enrichment.service

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.enrichment.converter.ExtendedItemConverter
import com.rarible.protocol.union.enrichment.converter.ExtendedOwnershipConverter
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.converter.ShortOwnershipConverter
import com.rarible.protocol.union.enrichment.event.ItemEventListener
import com.rarible.protocol.union.enrichment.event.ItemEventUpdate
import com.rarible.protocol.union.enrichment.event.OwnershipEventListener
import com.rarible.protocol.union.enrichment.event.OwnershipEventUpdate
import com.rarible.protocol.union.enrichment.model.ShortItemId
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RefreshService(
    private val itemService: ItemService,
    private val ownershipService: OwnershipService,
    private val orderService: OrderService,
    private val lockService: LockService,
    private val itemEventListeners: List<ItemEventListener>,
    private val ownershipEventListeners: List<OwnershipEventListener>
) {

    private val logger = LoggerFactory.getLogger(RefreshService::class.java)

    suspend fun refreshItemWithOwnerships(itemId: ShortItemId) = coroutineScope {
        logger.info("Starting full refresh of Item [{}] (with ownerships)", itemId)
        val ownerships = ownershipService.fetchAllByItemId(itemId)
        logger.info("Fetched {} Ownerships for Item [{}]", ownerships.size, itemId)
        ownerships
            .map { async { refreshOwnership(it) } }
            .map { it.await() }
        refreshItem(itemId)
    }

    suspend fun refreshItem(itemId: ShortItemId) = coroutineScope {
        logger.info("Starting refresh of Item [{}]", itemId)
        val itemDtoDeferred = async { itemService.fetch(itemId) }
        val bestSellOrderDeferred = async { orderService.getBestSell(itemId) }
        val bestBidOrderDeferred = async { orderService.getBestBid(itemId) }
        val unlockableDeferred = async { lockService.isUnlockable(itemId) }
        val sellStats = ownershipService.getItemSellStats(itemId)

        val itemDto = itemDtoDeferred.await()
        val bestSellOrder = bestSellOrderDeferred.await()
        val bestBidOrder = bestBidOrderDeferred.await()

        val short = ShortItemConverter.convert(itemDto).copy(
            bestBidOrder = bestBidOrderDeferred.await()?.let { ShortOrderConverter.convert(it) },
            bestSellOrder = bestSellOrderDeferred.await()?.let { ShortOrderConverter.convert(it) },
            unlockable = unlockableDeferred.await(),
            sellers = sellStats.sellers,
            totalStock = sellStats.totalStock
        )

        if (short.isNotEmpty()) {
            logger.info(
                "Saving refreshed Item [{}] with gathered enrichment data [{}]",
                itemId,
                short
            )
            optimisticLock {
                val currentVersion = itemService.get(itemId)?.version
                itemService.save(short.copy(version = currentVersion))
            }
        } else {
            logger.info("Item [{}] has no enrichment data: {}", itemId, short)
            itemService.delete(itemId)
        }

        val orders = listOfNotNull(bestSellOrder, bestBidOrder)
            .associateBy { it.id }

        val dto = ExtendedItemConverter.convert(itemDto, short, orders)
        val event = ItemEventUpdate(dto)

        itemEventListeners.forEach { it.onEvent(event) }
        dto
    }

    private suspend fun refreshOwnership(ownership: OwnershipDto) {
        val short = ShortOwnershipConverter.convert(ownership)
        val bestSellOrder = orderService.getBestSell(short.id)
        val enrichedOwnership = short.copy(
            bestSellOrder = bestSellOrder?.let { ShortOrderConverter.convert(it) }
        )

        if (enrichedOwnership.isNotEmpty()) {
            logger.info("Updating Ownership [{}] : {}", short.id, enrichedOwnership)
            ownershipService.save(enrichedOwnership)
        } else {
            val result = ownershipService.delete(short.id)
            // Nothing changed for this Ownership, event won't be sent
            if (result == null || result.deletedCount == 0L) {
                return
            }
        }

        val orders = listOfNotNull(bestSellOrder)
            .associateBy { it.id }

        val dto = ExtendedOwnershipConverter.convert(ownership, short, orders)
        val event = OwnershipEventUpdate(dto)

        ownershipEventListeners.forEach { it.onEvent(event) }
    }
}