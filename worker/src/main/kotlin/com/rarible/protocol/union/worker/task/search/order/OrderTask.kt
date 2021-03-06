package com.rarible.protocol.union.worker.task.search.order

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.core.model.EsOrder
import com.rarible.protocol.union.worker.config.OrderReindexProperties
import com.rarible.protocol.union.worker.task.search.ParamFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.springframework.stereotype.Component

@Component
class OrderTask(
    private val properties: OrderReindexProperties,
    private val paramFactory: ParamFactory,
    private val orderReindexService: OrderReindexService
) : TaskHandler<String> {

    override val type: String
        get() = EsOrder.ENTITY_DEFINITION.reindexTask

    override suspend fun isAbleToRun(param: String): Boolean {
        val blockchain = paramFactory.parse<OrderTaskParam>(param).blockchain
        return properties.isBlockchainActive(blockchain)
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val taskParam = paramFactory.parse<OrderTaskParam>(param)
        return if (from == "") {
            emptyFlow()
        } else {
            orderReindexService.reindex(
                taskParam.blockchain, taskParam.index, from
            )
        }
    }
}
