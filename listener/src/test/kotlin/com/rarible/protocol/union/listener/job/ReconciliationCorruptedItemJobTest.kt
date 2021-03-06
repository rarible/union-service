package com.rarible.protocol.union.listener.job

import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentRefreshService
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionBidOrderDto
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReconciliationCorruptedItemJobTest {

    private val refreshService: EnrichmentRefreshService = mockk()
    private val orderService: OrderService = mockk()
    private val orderServiceRouter: BlockchainRouter<OrderService> = mockk()
    private val itemRepository: ItemRepository = mockk()

    private val job = ReconciliationCorruptedItemJob(
        itemRepository,
        orderServiceRouter,
        refreshService
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(refreshService, orderServiceRouter, itemRepository)
        coEvery { orderServiceRouter.getService(BlockchainDto.ETHEREUM) } returns orderService
        coEvery { refreshService.reconcileItem(any(), true) } returns mockk() // doesn't matter
    }

    @Test
    fun `reconcile corrupted items`() = runBlocking<Unit> {
        val correctItemId = randomEthItemId()
        val correctBid = randomUnionBidOrderDto(correctItemId)
        val correctSell = randomUnionSellOrderDto(correctItemId)
        val correctItem = randomShortItem(correctItemId).copy(
            bestSellOrder = ShortOrderConverter.convert(correctSell),
            bestBidOrder = ShortOrderConverter.convert(correctBid)
        )

        val corruptedItemId = randomEthItemId()
        val corruptedBid = randomUnionBidOrderDto(corruptedItemId).copy(status = OrderStatusDto.INACTIVE)
        val corruptedItem = randomShortItem(corruptedItemId).copy(
            bestSellOrder = null,
            bestBidOrder = ShortOrderConverter.convert(corruptedBid)
        )

        val missedOrderItemId = randomEthItemId()
        val missedOrderSell = randomUnionSellOrderDto(missedOrderItemId)
        val missedOrderItem = randomShortItem(missedOrderItemId).copy(
            bestSellOrder = ShortOrderConverter.convert(missedOrderSell),
            bestBidOrder = null
        )

        // First page
        coEvery {
            itemRepository.findByBlockchain(null, BlockchainDto.ETHEREUM, 100)
        } returns flowOf(correctItem, corruptedItem, missedOrderItem)

        // Second page
        coEvery {
            itemRepository.findByBlockchain(ShortItemId(missedOrderItemId), BlockchainDto.ETHEREUM, 100)
        } returns emptyFlow()

        coEvery {
            orderService.getOrdersByIds(any())
        } returns listOf(correctBid, correctSell, corruptedBid)

        val next = job.reconcileCorruptedItems(null, BlockchainDto.ETHEREUM).toList()

        assertThat(next)
        coVerify(exactly = 0) { refreshService.reconcileItem(correctItemId, true) }
        coVerify(exactly = 1) { refreshService.reconcileItem(corruptedItemId, true) }
        coVerify(exactly = 1) { refreshService.reconcileItem(missedOrderItemId, true) }
        coVerify(exactly = 1) { orderService.getOrdersByIds(any()) }
    }
}