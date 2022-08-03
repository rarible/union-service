package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.common.flatMapAsync
import com.rarible.protocol.union.core.model.EsAllOrderFilter
import com.rarible.protocol.union.core.model.EsOrder
import com.rarible.protocol.union.core.model.EsOrderBidOrdersByItem
import com.rarible.protocol.union.core.model.EsOrderFilter
import com.rarible.protocol.union.core.model.EsOrderSellOrders
import com.rarible.protocol.union.core.model.EsOrderSellOrdersByItem
import com.rarible.protocol.union.core.model.EsOrdersByMakers
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.OrdersDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.repository.search.EsOrderRepository
import com.rarible.protocol.union.enrichment.service.query.order.OrderQueryService
import org.springframework.stereotype.Service

@Service
class OrderElasticService(
    private val router: BlockchainRouter<OrderService>,
    private val esOrderRepository: EsOrderRepository
) : OrderQueryService {

    @ExperimentalStdlibApi
    override suspend fun getOrdersAll(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?
    ): OrdersDto {
        val safeSize = PageSize.ORDER.limit(size)
        val evaluatedBlockchains = router.getEnabledBlockchains(blockchains)

        val orderFilter = EsAllOrderFilter(
            blockchains = evaluatedBlockchains,
            continuation = DateIdContinuation.parse(continuation),
            size = safeSize,
            sort = sort ?: OrderSortDto.LAST_UPDATE_DESC,
            status = status,
        )
        return fetchOrders(orderFilter)
    }

    override suspend fun getAllSync(
        blockchain: BlockchainDto,
        continuation: String?,
        size: Int?,
        sort: SyncSortDto?
    ): OrdersDto {
        throw UnsupportedOperationException("Operation is not supported for Elastic Search")
    }

    override suspend fun getSellOrdersByItem(
        itemId: String,
        platform: PlatformDto?,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int?
    ): OrdersDto {
        val safeSize = PageSize.ORDER.limit(size)

        val orderFilter = EsOrderSellOrdersByItem(
            itemId = itemId,
            platform = platform,
            maker = maker,
            origin = origin,
            status = status,
            continuation = DateIdContinuation.parse(continuation),
            size = safeSize,
            sort = OrderSortDto.LAST_UPDATE_DESC
        )
        return fetchOrders(orderFilter)
    }

    override suspend fun getOrderBidsByItem(
        itemId: String,
        platform: PlatformDto?,
        maker: List<String>?,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?
    ): OrdersDto {
        val safeSize = PageSize.ORDER.limit(size)

        val orderFilter = EsOrderBidOrdersByItem(
            itemId = itemId,
            platform = platform,
            maker = maker,
            origin = origin,
            status = status,
            continuation = DateIdContinuation.parse(continuation),
            size = safeSize,
            sort = OrderSortDto.LAST_UPDATE_DESC
        )
        return fetchOrders(orderFilter)
    }

    override suspend fun getOrderBidsByMaker(
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        maker: List<String>,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int?
    ): OrdersDto {
        val safeSize = PageSize.ORDER.limit(size)

        val orderFilter = EsOrdersByMakers(
            platform = platform,
            maker = maker,
            origin = origin,
            status = status,
            continuation = DateIdContinuation.parse(continuation),
            size = safeSize,
            sort = OrderSortDto.LAST_UPDATE_DESC,
            type = EsOrder.Type.BID
        )
        return fetchOrders(orderFilter)
    }

    override suspend fun getSellOrders(
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?
    ): OrdersDto {
        val safeSize = PageSize.ORDER.limit(size)

        val orderFilter = EsOrderSellOrders(
            blockchains = blockchains,
            platform = platform,
            origin = origin,
            continuation = DateIdContinuation.parse(continuation),
            size = safeSize,
            sort = OrderSortDto.LAST_UPDATE_DESC
        )
        return fetchOrders(orderFilter)
    }

    override suspend fun getSellOrdersByMaker(
        maker: List<String>,
        blockchains: List<BlockchainDto>?,
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int?,
        status: List<OrderStatusDto>?
    ): OrdersDto {
        val safeSize = PageSize.ORDER.limit(size)

        val orderFilter = EsOrdersByMakers(
            platform = platform,
            maker = maker,
            origin = origin,
            status = status,
            continuation = DateIdContinuation.parse(continuation),
            size = safeSize,
            sort = OrderSortDto.LAST_UPDATE_DESC,
            type = EsOrder.Type.SELL
        )
        return fetchOrders(orderFilter)
    }

    suspend fun fetchOrders(orderFilter: EsOrderFilter): OrdersDto {
        val esOrders = esOrderRepository.findByFilter(orderFilter)
        val orderIdsByBlockchain = esOrders.groupBy(EsOrder::blockchain, EsOrder::orderId)

        val slices = orderIdsByBlockchain.flatMapAsync { (blockchain, ids) ->
            val isBlockchainEnabled = router.isBlockchainEnabled(blockchain)
            if(isBlockchainEnabled) {
                val rawIds = ids.map { IdParser.parseOrderId(it).value }
                router.getService(blockchain).getOrdersByIds(rawIds)
            } else emptyList()
        }.associateBy { it.id.fullId() }

        val sortedOrders = esOrders.map { slices[it.orderId]!! }

        return if(esOrders.isEmpty()) {
            OrdersDto(orders = emptyList(), continuation = null)
        } else {
            val last = esOrders.last()
            OrdersDto(
                orders = sortedOrders,
                continuation = DateIdContinuation(
                    id = last.orderId,
                    date = last.lastUpdatedAt
                ).toString()
            )
        }
    }
}
