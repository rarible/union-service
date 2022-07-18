package com.rarible.protocol.union.integration.aptos.converter

import com.rarible.protocol.dto.aptos.TokenMetaDataDto
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.MetaAttributeDto
import com.rarible.protocol.union.dto.MetaContentDto
import org.slf4j.LoggerFactory

object AptosItemMetaConverter {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun convert(source: TokenMetaDataDto, blockchain: BlockchainDto): UnionMeta {
        return try {
            convertInternal(source, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} ItemMeta: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    private fun convertInternal(source: TokenMetaDataDto, blockchain: BlockchainDto): UnionMeta {
        return UnionMeta(
            name = source.name,
            description = source.description,
            attributes = source.attributes.map {
                MetaAttributeDto(
                    key = it.key,
                    value = it.value,
                    type = it.type,
                    format = it.format
                )
            },
            content = source.content.map {
                UnionMetaContent(
                    url = it.url,
                    representation = MetaContentDto.Representation.ORIGINAL,
                )
            },
        )
    }
}
