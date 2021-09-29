package com.rarible.protocol.union.core.flow.service

import com.rarible.protocol.flow.nft.api.client.FlowOrderControllerApi
import com.rarible.protocol.union.core.flow.converter.FlowOrderConverter
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrdersDto
import com.rarible.protocol.union.dto.PlatformDto
import kotlinx.coroutines.reactive.awaitFirst

class FlowOrderService(
    blockchain: BlockchainDto,
    private val orderControllerApi: FlowOrderControllerApi
) : AbstractFlowService(blockchain), OrderService {

    override suspend fun getOrdersAll(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int
    ): OrdersDto {
        return stub()
    }

    override suspend fun getOrderById(id: String): OrderDto {
        val order = orderControllerApi.getOrderByOrderId(id).awaitFirst()
        return FlowOrderConverter.convert(order, blockchain)
    }

    override suspend fun updateOrderMakeStock(id: String): OrderDto {
        // TODO implement when Flow support it
        val order = orderControllerApi.getOrderByOrderId(id).awaitFirst()
        return FlowOrderConverter.convert(order, blockchain)
    }

    override suspend fun getOrderBidsByItem(
        platform: PlatformDto?,
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        continuation: String?,
        size: Int
    ): OrdersDto {
        return stub()
    }

    override suspend fun getOrderBidsByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): OrdersDto {
        return stub()
    }

    override suspend fun getSellOrders(
        platform: PlatformDto?,
        origin: String?,
        continuation: String?,
        size: Int
    ): OrdersDto {
        return stub()
    }

    override suspend fun getSellOrdersByCollection(
        platform: PlatformDto?,
        collection: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): OrdersDto {
        return stub()
    }

    override suspend fun getSellOrdersByItem(
        platform: PlatformDto?,
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        continuation: String?,
        size: Int
    ): OrdersDto {
        return stub()
    }

    override suspend fun getSellOrdersByMaker(
        platform: PlatformDto?,
        maker: String,
        origin: String?,
        continuation: String?,
        size: Int
    ): OrdersDto {
        return stub()
    }

    // TODO remove when FLow support Order API
    private fun stub(): OrdersDto {
        return OrdersDto(
            continuation = null,
            orders = listOf()
        )
    }
}