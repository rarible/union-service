package com.rarible.protocol.union.worker.task.search.item

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.api.client.ItemControllerApi
import com.rarible.protocol.union.core.converter.EsItemConverter.toEsItem
import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.dto.continuation.CombinedContinuation
import com.rarible.protocol.union.dto.continuation.page.ArgSlice
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.worker.config.CollectionReindexProperties
import com.rarible.protocol.union.worker.metrics.SearchTaskMetricFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactor.awaitSingle

class ItemTask(
    private val properties: CollectionReindexProperties,
    private val client: ItemControllerApi,
    private val repository: EsItemRepository,
    private val searchTaskMetricFactory: SearchTaskMetricFactory
) : TaskHandler<String> {

    override val type: String
        get() = EsItem.ENTITY_DEFINITION.reindexTask

    override suspend fun isAbleToRun(param: String): Boolean {
        val blockchain = IdParser.parseBlockchain(param)
        return properties.enabled && properties.blockchains.single { it.blockchain == blockchain }.enabled
    }

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val blockchain = IdParser.parseBlockchain(param)
        val counter = searchTaskMetricFactory.createReindexItemCounter(blockchain)
        return if (from == "") {
            emptyFlow()
        } else {
            var continuation = from
            flow {
                do {
                    val res = client.getAllItems(
                        listOf(blockchain),
                        continuation,
                        PageSize.ITEM.max,
                        true,
                        Long.MIN_VALUE,
                        Long.MAX_VALUE
                    ).awaitSingle()

                    if (res.items.isNotEmpty()) {
                        repository.saveAll(
                            res.items.map { it.toEsItem() }
                        )
                        counter.increment(res.items.size)
                    }
                    emit(res.continuation.orEmpty())
                    val continuations = CombinedContinuation.parse(res.continuation).continuations

                    val stop = continuations.isEmpty() || continuations.all { it.value == ArgSlice.COMPLETED }
                    continuation = res.continuation
                } while (!stop)
            }
        }
    }
}