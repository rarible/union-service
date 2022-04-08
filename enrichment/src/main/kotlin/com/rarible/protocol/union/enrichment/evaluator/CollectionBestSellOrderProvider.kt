package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.enrichment.model.ShortCollection
import com.rarible.protocol.union.enrichment.model.ShortCollectionId
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderService

class CollectionBestSellOrderProvider(
    private val collectionId: ShortCollectionId,
    private val currencyId: String,
    private val enrichmentOrderService: EnrichmentOrderService
) : BestOrderProvider<ShortCollection> {

    override val entityId: String = collectionId.toString()
    override val entityType: Class<ShortCollection> get() = ShortCollection::class.java

    override suspend fun fetch(): OrderDto? {
        return enrichmentOrderService.getBestSell(collectionId, currencyId)
    }
}