package com.rarible.protocol.union.worker.task.search.order

import com.rarible.protocol.union.core.converter.EsOrderConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.enrichment.repository.search.EsOrderRepository
import com.rarible.protocol.union.enrichment.service.query.order.OrderApiMergeService
import com.rarible.protocol.union.worker.metrics.SearchTaskMetricFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.elasticsearch.action.support.WriteRequest
import org.springframework.stereotype.Service

@Service
class OrderReindexService(
    private val orderApiMergeService: OrderApiMergeService,
    private val repository: EsOrderRepository,
    private val searchTaskMetricFactory: SearchTaskMetricFactory
) {

    fun reindex(
        blockchain: BlockchainDto,
        index: String?,
        cursor: String? = null
    ): Flow<String> {
        val counter = searchTaskMetricFactory.createReindexOrderCounter(blockchain)
        var continuation = cursor
        return flow {
            do {
                val res = orderApiMergeService.getOrdersAll(
                    listOf(blockchain),
                    continuation,
                    PageSize.ORDER.max,
                    OrderSortDto.LAST_UPDATE_DESC,
                    OrderStatusDto.values().asList()
                )

                if (res.orders.isNotEmpty()) {
                    repository.saveAll(
                        res.orders.map { EsOrderConverter.convert(it) },
                        refreshPolicy = WriteRequest.RefreshPolicy.NONE
                    )
                    counter.increment(res.orders.size)
                }
                emit(res.continuation.orEmpty())
                continuation = res.continuation
            } while (continuation != null)
        }
    }
}