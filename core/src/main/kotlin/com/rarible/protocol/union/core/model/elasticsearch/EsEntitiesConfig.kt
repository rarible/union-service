package com.rarible.protocol.union.core.model.elasticsearch

import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.EsCollection
import com.rarible.protocol.union.core.model.EsOrder
import com.rarible.protocol.union.core.model.EsOwnership
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object EsEntitiesConfig {

    fun createEsEntities(): List<EntityDefinition> = listOf(
        EsActivity.ENTITY_DEFINITION,
        EsCollection.ENTITY_DEFINITION,
        EsOrder.ENTITY_DEFINITION,
        EsOwnership.ENTITY_DEFINITION,
    )

    fun loadMapping(entityName: String): String =
        try {
            EsEntitiesConfig::class.java.getResource("/mappings/$entityName.json")!!.readText()
        } catch (e: Exception) {
            logger.error("Not found mapping for index $entityName")
            throw e
        }

    val logger: Logger = LoggerFactory.getLogger(EsEntitiesConfig::class.java)
}