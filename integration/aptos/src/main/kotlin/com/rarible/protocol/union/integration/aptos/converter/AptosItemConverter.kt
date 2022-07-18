package com.rarible.protocol.union.integration.aptos.converter

import com.rarible.protocol.dto.aptos.TokenDto
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.group
import java.math.BigInteger
import java.time.Instant
import org.slf4j.LoggerFactory

object AptosItemConverter {

    private val logger = LoggerFactory.getLogger(AptosItemConverter::class.java)

    fun convert(item: TokenDto, blockchain: BlockchainDto): UnionItem {
        try {
            return convertInternal(item, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Item: {} \n{}", blockchain, e.message, item)
            throw e
        }
    }

    private fun convertInternal(item: TokenDto, blockchain: BlockchainDto): UnionItem {
        val (creator, collectionName, _) = item.id.split("::")
        return UnionItem(
            id = ItemIdDto(
                blockchain = blockchain,
                value = item.id
            ),
            collection = CollectionIdDto(blockchain, "${creator}::$collectionName"),
            creators = listOf(CreatorDto(account = UnionAddress(blockchain.group(), item.creator), 1)),
            lazySupply = BigInteger.ZERO,
            mintedAt = item.mintedAt,
            deleted = item.deleted ?: false,
            supply = item.supply.toBigInteger(),
            lastUpdatedAt = Instant.now()
        )
    }
}
