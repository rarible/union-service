package com.rarible.protocol.union.worker.task

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.worker.config.ActivityReindexProperties
import com.rarible.protocol.union.worker.config.BlockchainReindexProperties
import com.rarible.protocol.union.worker.task.search.ParamFactory
import com.rarible.protocol.union.worker.task.search.activity.ActivityReindexService
import com.rarible.protocol.union.worker.task.search.activity.ActivityTask
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class ActivityTaskUnitTest {

    private val service = mockk<ActivityReindexService> {
        coEvery {
            reindex(any(), any(), "activity_test_index", any())
        } returns flowOf("next_cursor")
    }

    private val taskRepository = mockk<TaskRepository> {
        coEvery {
            findByTypeAndParam(any(), any())
        } returns mono { Task(type = "", param = "", running = true) }
    }

    @Test
    fun `should launch first run of the task`(): Unit {
        runBlocking {
            val task = ActivityTask(
                ActivityReindexProperties(
                    enabled = true,
                    blockchains = listOf(BlockchainReindexProperties(enabled = true, BlockchainDto.ETHEREUM))
                ),
                ParamFactory(jacksonObjectMapper().registerKotlinModule()),
                service,
                taskRepository
            )

            task.runLongTask(
                null,
                """{"blockchain": "ETHEREUM", "type": "LIST", "index":"activity_test_index"}"""
            ).toList()

            coVerify {
                service.reindex(BlockchainDto.ETHEREUM, ActivityTypeDto.LIST, "activity_test_index", null)
            }
        }
    }

    @Test
    fun `should launch next run of the task`(): Unit = runBlocking {
        val task = ActivityTask(
            ActivityReindexProperties(
                enabled = true,
                blockchains = listOf(BlockchainReindexProperties(enabled = true, BlockchainDto.ETHEREUM))
            ),
            ParamFactory(jacksonObjectMapper().registerKotlinModule()),
            service,
            taskRepository
        )

        task.runLongTask(
            "ETHEREUM:cursor_1",
            """{"blockchain": "ETHEREUM", "type": "LIST", "index":"activity_test_index"}"""
        ).toList()

        coVerify {
            service.reindex(BlockchainDto.ETHEREUM, ActivityTypeDto.LIST, "activity_test_index", "ETHEREUM:cursor_1")
        }
    }
}