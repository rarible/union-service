package com.rarible.protocol.union.api.service

import com.rarible.core.common.nowMillis
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.dto.ActivitiesDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import java.time.Instant
import javax.annotation.PreDestroy

@Service
class DiscrepancyDetector {

    companion object {
        private val logger by Logger()
    }

    private data class DiscrepancyCheck<T>(
        val elasticResult: List<T>,
        val apiMergeResultCall: suspend () -> List<T>,
        val timestamp: Instant,
    )

    private val channel = Channel<DiscrepancyCheck<*>>(capacity = 10, BufferOverflow.DROP_OLDEST)

    @PreDestroy
    fun destroy() {
        channel.close()
    }

    init {
        GlobalScope.launch(Dispatchers.IO) {
            for (discrepancyCheck in channel) {
                if (nowMillis().minusMillis(5000) > discrepancyCheck.timestamp) {
                    logger.info("Result is too old, skipping")
                    continue
                }

                if (discrepancyCheck.elasticResult.size > 50) {
                    logger.info("Too many results, skipping: ${discrepancyCheck.elasticResult.size}")
                    continue
                }

                val apiMergeResult = discrepancyCheck.apiMergeResultCall()

                if (discrepancyCheck.elasticResult != apiMergeResult) {
                    logger.error("Discrepancy detected: $discrepancyCheck.elasticResult != $apiMergeResult")
                    // write some metrics
                } else {
                    logger.info("Discrepancy check passed")
                }
            }
        }
    }

    suspend fun planActivityDiscrepancyCheck(
        elasticResult: ActivitiesDto,
        apiMergeResultCall: suspend () -> ActivitiesDto,
        timestamp: Instant,
    ) = planDiscrepancyCheck(elasticResult.activities, { apiMergeResultCall().activities }, timestamp)

    private suspend fun <T> planDiscrepancyCheck(
        elasticResult: List<T>,
        apiMergeResultCall: suspend () -> List<T>,
        timestamp: Instant,
    ) = coroutineScope {
        launch(Dispatchers.IO) {
            channel.send(DiscrepancyCheck(elasticResult, apiMergeResultCall, timestamp))
        }
    }
}
