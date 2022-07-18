package com.rarible.protocol.union.worker.task.search.collection

import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.core.model.EsCollection
import com.rarible.protocol.union.worker.config.CollectionReindexProperties
import com.rarible.protocol.union.worker.task.search.CollectionTaskParam
import com.rarible.protocol.union.worker.task.search.ParamFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Component

@Component
class CollectionTask(
    private val properties: CollectionReindexProperties,
    private val paramFactory: ParamFactory,
    private val collectionReindexService: CollectionReindexService,
    private val taskRepository: TaskRepository,
) : TaskHandler<String> {

    override val type: String
        get() = EsCollection.ENTITY_DEFINITION.reindexTask

    override suspend fun isAbleToRun(param: String): Boolean {
        val parameter = paramFactory.parse<CollectionTaskParam>(param)
        return properties.isBlockchainActive(parameter.blockchain)
    }

    /**
     * from - cursor
     * param is json-serialized ColllectionTaskParam
     */
    override fun runLongTask(from: String?, param: String): Flow<String> {
        return if (from == "") {
            emptyFlow()
        } else {
            val parameter = paramFactory.parse<CollectionTaskParam>(param)
            return collectionReindexService.reindex(
                parameter.blockchain,
                parameter.index,
                from
            )
                .takeWhile { taskRepository.findByTypeAndParam(type, param).awaitSingleOrNull()?.running ?: false }
        }
    }
}
