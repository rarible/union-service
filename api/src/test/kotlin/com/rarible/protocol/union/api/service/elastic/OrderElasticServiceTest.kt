package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.core.converter.EsOrderConverter
import com.rarible.protocol.union.core.model.EsAllOrderFilter
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.enrichment.repository.search.EsOrderRepository
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.service.EthOrderService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import randomOrder
import randomOrderId

internal class OrderElasticServiceTest {

    val ethOrders = listOf(
        randomOrder().copy(id = randomOrderId(BlockchainDto.ETHEREUM)),
        randomOrder().copy(id = randomOrderId(BlockchainDto.ETHEREUM)),
        randomOrder().copy(id = randomOrderId(BlockchainDto.ETHEREUM))
    )

    val ethereumOrderService = mockk<EthOrderService> {
        coEvery {
            getOrdersByIds(any())
        } returns ethOrders
    }

    val router = mockk<BlockchainRouter<OrderService>> {
        every {
            getEnabledBlockchains(any())
        } answers { arg(0) }

        every {
            isBlockchainEnabled(any())
        } returns true

        every {
            getService(BlockchainDto.ETHEREUM)
        } returns ethereumOrderService
    }

    val esOrderRepository = mockk<EsOrderRepository> {
        coEvery {
            findByFilter(any())
        } returns ethOrders.map { EsOrderConverter.convert(it) }.sortedBy { it.lastUpdatedAt }
    }

    val orderElasticService = OrderElasticService(router, esOrderRepository)

    @Test
    fun fetchOrders() = runBlocking<Unit> {
        val result = orderElasticService.fetchOrders(
            EsAllOrderFilter(
                blockchains = listOf(BlockchainDto.ETHEREUM),
                continuation = null,
                size = 100,
                status = null,
                sort = OrderSortDto.LAST_UPDATE_DESC
            )
        )

        Assertions.assertThat(result.orders).containsExactlyElementsOf(
            ethOrders.sortedBy { it.lastUpdatedAt }
        )
    }
}