package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.OrdersPaginationDto
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthOpenSeaV1OrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthSellOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthV2OrderDto
import com.rarible.protocol.union.test.mock.CurrencyMock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toMono

class EthOrderServiceTest {

    private val orderControllerApi: OrderControllerApi = mockk()
    private val converter = EthOrderConverter(CurrencyMock.currencyServiceMock)
    private val service = EthereumOrderService(orderControllerApi, converter)

    @Test
    fun `ethereum get all`() = runBlocking<Unit> {
        val order1 = randomEthSellOrderDto()
        val order2 = randomEthV2OrderDto()

        val continuation = randomString()
        val size = randomInt()

        val expected1 = converter.convert(order1, BlockchainDto.ETHEREUM)
        val expected2 = converter.convert(order2, BlockchainDto.ETHEREUM)

        coEvery {
            orderControllerApi.getOrdersAllByStatus(any(), continuation, size, any())
        } returns OrdersPaginationDto(listOf(order1, order2)).toMono()

        val result = service.getOrdersAll(
            continuation,
            size,
            null,
            null
        )

        assertThat(result.entities).hasSize(2)
        assertThat(result.entities[0]).isEqualTo(expected1)
        assertThat(result.entities[1]).isEqualTo(expected2)
    }

    @Test
    fun `ethereum get all - skip when only historical status is given`() = runBlocking<Unit> {
        // given
        val continuation = randomString()
        val size = randomInt()

        // when
        val result = service.getOrdersAll(
            continuation,
            size,
            null,
            listOf(OrderStatusDto.HISTORICAL)
        )

        // then
        assertThat(result.entities).isEmpty()
        assertThat(result.continuation).isNull()
        confirmVerified(orderControllerApi)
    }

    @Test
    fun `ethereum get by id`() = runBlocking<Unit> {
        val order = randomEthOpenSeaV1OrderDto()
        val orderId = EthConverter.convert(order.hash)
        val expected = converter.convert(order, BlockchainDto.ETHEREUM)

        coEvery { orderControllerApi.getOrderByHash(orderId) } returns order.toMono()

        val result = service.getOrderById(orderId)

        assertThat(result).isEqualTo(expected)
    }

}
