package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsTrait
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ext

object EsItemConverter {

    fun ItemDto.toEsItem(): EsItem {
        return EsItem(
            itemId = id.fullId(),
            blockchain = blockchain,
            collection = collection?.fullId(),
            name = meta?.name,
            description = meta?.description,
            creators = creators.map { it.account.fullId() },
            mintedAt = mintedAt,
            lastUpdatedAt = lastUpdatedAt,
            deleted = deleted,
            traits = meta?.attributes?.map { EsTrait(it.key, it.value) } ?: emptyList(),
            self = self,
            bestSellAmount = this.bestSellOrder?.take?.value?.toDouble(),
            bestSellCurrency = getCurrencyAddressOrNull(this.bestSellOrder?.take),
            bestSellMarketplace = this.bestSellOrder?.platform?.name, // getting marketplace may be more complicated
            bestBidAmount = this.bestBidOrder?.make?.value?.toDouble(),
            bestBidCurrency = getCurrencyAddressOrNull(this.bestBidOrder?.make),
            bestBidMarketplace = this.bestBidOrder?.platform?.name, // getting marketplace may be more complicated
        )
    }

    private fun getCurrencyAddressOrNull(asset: AssetDto?): String? {
        return kotlin.runCatching {
            asset?.type?.ext?.currencyAddress()
        }.getOrNull()
    }
}
