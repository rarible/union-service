package com.rarible.protocol.union.worker.task.search.item

import com.rarible.core.task.TaskHandler
import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.core.converter.EsItemConverter.toEsItem
import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.dto.continuation.CombinedContinuation
import com.rarible.protocol.union.dto.continuation.page.ArgSlice
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.service.query.item.ItemApiMergeService
import com.rarible.protocol.union.worker.config.ItemReindexProperties
import com.rarible.protocol.union.worker.metrics.SearchTaskMetricFactory
import com.rarible.protocol.union.worker.task.search.ItemTaskParam
import com.rarible.protocol.union.worker.task.search.ParamFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.elasticsearch.action.support.WriteRequest
import org.springframework.stereotype.Component

@Component
class ItemTask(
    private val properties: ItemReindexProperties,
    private val itemApiMergeService: ItemApiMergeService,
    private val paramFactory: ParamFactory,
    private val repository: EsItemRepository,
    private val searchTaskMetricFactory: SearchTaskMetricFactory,
    private val taskRepository: TaskRepository,
) : TaskHandler<String> {

    override val type: String
        get() = EsItem.ENTITY_DEFINITION.reindexTask

    override suspend fun isAbleToRun(param: String): Boolean {
        val blockchain = paramFactory.parse<ItemTaskParam>(param).blockchain
        return properties.enabled && properties.blockchains.single { it.blockchain == blockchain }.enabled
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val blockchain = paramFactory.parse<ItemTaskParam>(param).blockchain
        val counter = searchTaskMetricFactory.createReindexItemCounter(blockchain)
        return if (from == "") {
            emptyFlow()
        } else {
            var continuation = from
            flow {
                do {
                    val res = itemApiMergeService.getAllItems(
                        listOf(blockchain),
                        continuation,
                        PageSize.ITEM.max,
                        true,
                        Long.MIN_VALUE,
                        Long.MAX_VALUE
                    )

                    if (res.items.isNotEmpty()) {
                        repository.saveAll(
                            res.items.map { it.toEsItem() },
                            refreshPolicy = WriteRequest.RefreshPolicy.NONE
                        )
                        counter.increment(res.items.size)
                    }
                    emit(res.continuation.orEmpty())
                    val continuations = CombinedContinuation.parse(res.continuation).continuations

                    val stop = continuations.isEmpty() || continuations.all { it.value == ArgSlice.COMPLETED }
                    continuation = res.continuation
                } while (!stop)
            }
                .takeWhile { taskRepository.findByTypeAndParam(type, param).awaitSingleOrNull()?.running ?: false }
        }
    }
}
