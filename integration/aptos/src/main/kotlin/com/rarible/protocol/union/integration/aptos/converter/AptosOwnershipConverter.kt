package com.rarible.protocol.union.integration.aptos.converter

import com.rarible.protocol.dto.aptos.OwnershipDto
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.group
import java.math.BigInteger
import java.time.Instant
import org.slf4j.LoggerFactory

object AptosOwnershipConverter {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun convert(source: OwnershipDto, blockchain: BlockchainDto): UnionOwnership {
        return try {
            convertInternal(source, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Ownership: {} \n{}", blockchain, e.message, source)
            throw e

        }
    }

    private fun convertInternal(source: OwnershipDto, blockchain: BlockchainDto): UnionOwnership {
        return UnionOwnership(
            id = OwnershipIdDto(
                blockchain = blockchain,
                itemIdValue = source.tokenId,
                owner = UnionAddress(blockchain.group(), source.owner)
            ),
            collection = CollectionIdDto(blockchain, source.collection),
            value = source.value.toBigInteger(),
            createdAt = source.createdAt,
            lazyValue = BigInteger.ZERO,
            creators = listOf(CreatorDto(
                account = UnionAddress(blockchain.group(), source.creator),
                value = 1
            )),
            lastUpdatedAt = Instant.now()
        )
    }


}
