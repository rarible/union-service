package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinition
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig.loadMapping
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig.loadSettings
import com.rarible.protocol.union.core.model.elasticsearch.EsEntity
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.Instant

data class EsItem(
    @Id
    val itemId: String,
    val blockchain: BlockchainDto,
    val collection: String?,
    val deleted: Boolean = false,

    val name: String?,
    val description: String?,
    val traits: List<EsTrait>,

    val creators: List<String>,

    @Field(type = FieldType.Date)
    val mintedAt: Instant,

    @Field(type = FieldType.Date)
    val lastUpdatedAt: Instant,
    val self: Boolean? = false
) {
    companion object {
        const val VERSION: Int = 1

        val ENTITY_DEFINITION = EsEntity.ITEM.let {
            EntityDefinition(
                entity = it,
                mapping = loadMapping(it),
                versionData = VERSION,
                settings = loadSettings(it)
            )
        }
    }
}

data class EsTrait(val key: String, val value: String?)
