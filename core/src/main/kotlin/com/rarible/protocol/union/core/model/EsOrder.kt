package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinition
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig.INDEX_SETTINGS
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig.loadMapping
import com.rarible.protocol.union.core.model.elasticsearch.EsEntity
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.UnionAddress
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.math.BigDecimal
import java.time.Instant

data class EsOrder(
    @Id
    val orderId: String, // blockchain:value

    @Field(type = FieldType.Date)
    val lastUpdatedAt: Instant,

    val type: Type,
    val blockchain: BlockchainDto,
    val platform: PlatformDto,
    val maker: String,
    val make: Asset,
    val taker: String?,
    val take: Asset,
    val start: Instant?,
    val end: Instant?,
    val origins: List<String>, //list of address fulIds
    val status: OrderStatusDto,
) {

    data class Asset(
        val address: String,
        val isNft: Boolean,
        val value: BigDecimal
    )

    enum class Type {
        SELL, BID
    }

    companion object {
        private const val VERSION = 1

        val ENTITY_DEFINITION = EsEntity.ORDER.let {
            EntityDefinition(
                entity = it,
                mapping = loadMapping(it),
                versionData = VERSION,
                settings = INDEX_SETTINGS
            )
        }
    }
}
