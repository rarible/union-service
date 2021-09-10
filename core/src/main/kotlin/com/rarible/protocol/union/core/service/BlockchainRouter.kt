package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.dto.BlockchainDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory

abstract class BlockchainRouter<T : BlockchainService>(
    private val services: List<T>
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val blockchainServices = services.associateBy { it.getBlockchain() }
    private val supportedBlockchains = BlockchainDto.values().toSet()

    fun getService(blockchain: BlockchainDto): T {
        return blockchainServices[blockchain] ?: throw IllegalArgumentException(
            "Operation is not supported for '$blockchain', next blockchains available for it: ${blockchainServices.keys}"
        )
    }

    suspend fun <R : Any> executeForAll(
        blockchains: Collection<BlockchainDto>?,
        clientCall: suspend (service: T) -> R
    ) = coroutineScope {
        val selectedServices = if (blockchains == null || blockchains.isEmpty()) {
            services
        } else {
            blockchains.map { getService(it) }
        }

        selectedServices.map {
            async {
                safeApiCall { clientCall(it) }
            }
        }.mapNotNull { it.await() }
    }

    private suspend fun <T> safeApiCall(
        clientCall: suspend () -> T
    ): T? {
        return try {
            clientCall()
        } catch (e: Exception) {
            logger.error("Unexpected exception during HTTP call: ", e)
            null
        }
    }
}