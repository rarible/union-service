package com.rarible.protocol.union.core.elasticsearch

import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended

interface ReindexSchedulingService {

    suspend fun stopTasksIfExists(
        entityDefinition: EntityDefinitionExtended
    ) {
    }

    suspend fun checkReindexInProgress(
        entityDefinition: EntityDefinitionExtended
    ): Boolean = false

    suspend fun scheduleReindex(
        newIndexName: String,
        entityDefinition: EntityDefinitionExtended
    )
}
