package com.rarible.protocol.union.worker.task.search.activity

import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.worker.config.ActivityReindexProperties
import com.rarible.protocol.union.worker.task.search.ActivityTaskParam
import com.rarible.protocol.union.worker.task.search.ParamFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Component

@Component
class ActivityTask(
    private val properties: ActivityReindexProperties,
    private val paramFactory: ParamFactory,
    private val activityReindexService: ActivityReindexService,
    private val taskRepository: TaskRepository,
): TaskHandler<String> {

    override val type: String
        get() = EsActivity.ENTITY_DEFINITION.reindexTask

    override suspend fun isAbleToRun(param: String): Boolean {
        val blockchain = paramFactory.parse<ActivityTaskParam>(param).blockchain
        return properties.isBlockchainActive(blockchain)
    }

    /**
     * from - cursor
     * param is json-serialized ActivityTaskParam
     */
    override fun runLongTask(from: String?, param: String): Flow<String> {
        return if(from == "") {
            emptyFlow()
        } else {
            val taskParam = paramFactory.parse<ActivityTaskParam>(param)
            return activityReindexService
                .reindex(
                    blockchain = taskParam.blockchain,
                    type = taskParam.type,
                    index = taskParam.index,
                    cursor = from
                )
                .takeWhile { taskRepository.findByTypeAndParam(type, param).awaitSingleOrNull()?.running ?: false }
        }
    }

}
