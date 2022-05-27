package com.rarible.protocol.union.worker.task.search.activity

import com.rarible.core.logging.Logger
import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.worker.metrics.SearchTaskMetricFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import randomUnionAddress
import java.math.BigInteger
import java.time.Instant

internal class ActivityReindexServiceTest {

    private val logger by Logger()

    private val counter = mockk<RegisteredCounter> {
        every {
            increment(any())
        } returns Unit
    }

    private val searchTaskMetricFactory = mockk<SearchTaskMetricFactory> {
        every {
            createReindexActivityCounter(any(), any())
        } returns counter
    }

    private val esRepo = mockk<EsActivityRepository> {
        coEvery {
            saveAll(any(), any())
        } answers { arg(0) }
    }

    @Test
    fun `should skip reindexing if there's nothing to reindex`() = runBlocking<Unit> {
        val service = ActivityReindexService(
            mockk {
                coEvery {
                    getAllActivities(any(), any(), any(), any(), any(), any())
                } returns ActivitiesDto(
                    null, null, emptyList()
                )
            }, esRepo, searchTaskMetricFactory
        )

        Assertions.assertThat(
            service
                .reindex(BlockchainDto.FLOW, ActivityTypeDto.CANCEL_LIST, "test_index")
                .toList()
        ).isEmpty()

        coVerify(exactly = 0) {
            esRepo.saveAll(emptyList(), "test_index")
            counter.increment(0)
        }
    }

    @Test
    fun `should reindex two rounds`() = runBlocking<Unit> {
        val service = ActivityReindexService(
            mockk {
                coEvery {
                    getAllActivities(listOf(ActivityTypeDto.CANCEL_LIST), listOf(BlockchainDto.ETHEREUM), eq("step_1"), eq("step_1"), any(), any())
                } answers {
                    logger.info("Step 1")
                    ActivitiesDto(
                        null, null, listOf(
                            randomActivityDto()
                        )
                    )
                }

                coEvery {
                    getAllActivities(listOf(ActivityTypeDto.CANCEL_LIST), listOf(BlockchainDto.ETHEREUM), null, null, any(), any())
                } returns ActivitiesDto(
                    "step_1", "step_1", listOf(
                        randomActivityDto()
                    )
                )
            }, esRepo, searchTaskMetricFactory
        )

        Assertions.assertThat(
            service
                .reindex(BlockchainDto.ETHEREUM, ActivityTypeDto.CANCEL_LIST, "test_index")
                .toList()
        ).containsExactly("step_1", "") // an empty string is always emitted in the end of loop

        coVerify(exactly = 2) {
            esRepo.saveAll(any(), "test_index")
            counter.increment(1)
        }
    }

    private fun randomActivityDto(): MintActivityDto {
        return MintActivityDto(
            id = ActivityIdDto(BlockchainDto.ETHEREUM, randomString()),
            Instant.now(),
            owner = randomUnionAddress(),
            value = BigInteger.ONE,
            transactionHash = randomString()
        )
    }

}