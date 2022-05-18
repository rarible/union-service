package com.rarible.protocol.union.worker.task

import com.rarible.core.logging.Logger
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.worker.config.OwnershipReindexProperties
import com.rarible.protocol.union.worker.task.search.ParamFactory
import com.rarible.protocol.union.worker.task.search.ownership.OwnershipReindexService
import com.rarible.protocol.union.worker.task.search.ownership.OwnershipTaskParam
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onCompletion

class OwnershipTask(
    private val properties: OwnershipReindexProperties,
    private val paramFactory: ParamFactory,
    private val reindexService: OwnershipReindexService,
    private val repository: EsOwnershipRepository,
    private val indexService: IndexService,
) : TaskHandler<String> {
    private val entityDefinition = repository.entityDefinition

    override val type: String
        get() = EsOwnership.ENTITY_DEFINITION.reindexTask

    override suspend fun isAbleToRun(param: String): Boolean {
        val blockchain = paramFactory.parse<OwnershipTaskParam>(param).blockchain
        return properties.enabled && properties.blockchains.single { it.blockchain == blockchain }.enabled
    }

    override fun runLongTask(from: String?, param: String): Flow<String> = when (from) {
        "" -> emptyFlow()
        else -> {
            val taskParam = paramFactory.parse<OwnershipTaskParam>(param)
            reindexService.reindex(
                blockchain = taskParam.blockchain,
                index = taskParam.index,
                cursor = from,
            ).onCompletion {
                indexService.finishIndexing(taskParam.index, entityDefinition)
                repository.refresh()
                logger.info("Finished reindex of ${entityDefinition.entity} with param $param")
            }
        }
    }

    companion object {
        private val logger by Logger()
    }
}
