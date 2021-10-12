package com.rarible.protocol.union.core.service.router

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.dto.BlockchainDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory

class BlockchainRouter<T : BlockchainService>(
    private val services: List<T>
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val blockchainServices = services.associateBy { it.blockchain }

    fun getService(blockchain: BlockchainDto): T {
        return blockchainServices[blockchain] ?: throw IllegalArgumentException(
            "Operation is not supported for '$blockchain', next blockchains available for it: ${blockchainServices.keys}"
        )
    }

    suspend fun <R : Any> executeForAll(
        blockchains: Collection<BlockchainDto>?,
        clientCall: suspend (service: T) -> R
    ): List<R> = coroutineScope {
        val selectedServices = if (blockchains == null || blockchains.isEmpty()) {
            services
        } else {
            blockchains.map { getService(it) }
        }

        selectedServices.map {
            async {
                safeApiCall { clientCall(it) }
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun <T> safeApiCall(
        clientCall: suspend () -> T
    ): T? {
        return try {
            clientCall()
        } catch (e: WebClientResponseProxyException) {
            logger.warn("Received an error from API Client: {} with message: {}", e.data, e.message)
            null
        } catch (e: Throwable) {
            logger.error("Unexpected error from API Client: {}", e.message, e)
            null
        }
    }
}
