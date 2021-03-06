package com.rarible.protocol.union.worker.task.search.order

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.worker.config.BlockchainReindexProperties
import com.rarible.protocol.union.worker.config.OrderReindexProperties
import com.rarible.protocol.union.worker.task.search.ParamFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyAll
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import randomOrder

class OrderTaskUnitTest {

    private val orderDto = randomOrder()

    private val paramFactory = ParamFactory(jacksonObjectMapper())

    private val service = mockk<OrderReindexService> {
        coEvery {
            reindex(any(), "test_index", any())
        } returns flowOf("next_cursor")
    }

    @Test
    internal fun `should start first task`() = runBlocking {
        val task = OrderTask(
            OrderReindexProperties(
                enabled = true,
                blockchains = listOf(BlockchainReindexProperties(enabled = true, BlockchainDto.ETHEREUM))
            ),
            paramFactory,
            service
        )
        task.runLongTask(
            null, paramFactory.toString(
                OrderTaskParam(
                    blockchain = BlockchainDto.ETHEREUM, index = "test_index"
                )
            )
        ).toList()

        coVerifyAll {
            service.reindex(
                BlockchainDto.ETHEREUM, "test_index", null
            )
        }
    }

    @Test
    fun `should launch next run of the task`(): Unit = runBlocking {
        val task = OrderTask(
            OrderReindexProperties(
                enabled = true,
                blockchains = listOf(BlockchainReindexProperties(enabled = true, BlockchainDto.ETHEREUM))
            ),
            paramFactory,
            service
        )

        task.runLongTask(
            orderDto.id.fullId(),
            paramFactory.toString(
                OrderTaskParam(
                    blockchain = BlockchainDto.ETHEREUM, index = "test_index"
                )
            )
        ).toList()

        coVerify {
            service.reindex(
                BlockchainDto.ETHEREUM, "test_index", orderDto.id.fullId()
            )
        }
    }
}
