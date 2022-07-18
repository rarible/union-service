package com.rarible.protocol.union.integration.aptos.converter

import com.rarible.protocol.dto.aptos.CollectionDto
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.group
import org.slf4j.LoggerFactory

object AptosCollectionConverter {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun convert(source: CollectionDto, blockchain: BlockchainDto): UnionCollection {
        try {
            return convertInternal(source, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Collection: {} \n{}", blockchain, e.message, source)
            throw e

        }
    }

    private fun convertInternal(source: CollectionDto, blockchain: BlockchainDto): UnionCollection {
        return UnionCollection(
            id = CollectionIdDto(blockchain, source.id),
            name = source.name,
            type = com.rarible.protocol.union.dto.CollectionDto.Type.APTOS,
            features = listOf(com.rarible.protocol.union.dto.CollectionDto.Features.APPROVE_FOR_ALL),
            owner = UnionAddress(blockchain.group(), source.creator),
            meta = UnionCollectionMeta(
                name = source.name,
                description = source.description,
            )
        )
    }
}
