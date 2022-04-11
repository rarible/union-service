package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexAsset
import java.math.BigDecimal
import java.math.BigInteger
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scalether.domain.Address

object ImmutablexItemConverter {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun convert(asset: ImmutablexAsset, blockchain: BlockchainDto): UnionItem {
        return try {
            convertInternal(asset, blockchain)
        } catch (e: Exception) {
            logger.error("Convert immutable x asset \n$asset\n is failed! \n${e.message}", e)
            throw e
        }
    }

    private fun convertInternal(asset: ImmutablexAsset, blockchain: BlockchainDto): UnionItem {
        val deleted = asset.user!! == "${Address.ZERO()}"
        return UnionItem(
            id = ItemIdDto(BlockchainDto.IMMUTABLEX, contract = asset.tokenAddress, tokenId = asset.tokenId.toBigInteger()),
            collection = CollectionIdDto(blockchain, asset.tokenAddress),
            creators = emptyList(),
            owners = emptyList(),
            royalties = asset.fees.map {
                RoyaltyDto(
                    account = UnionAddressConverter.convert(ContractAddressConverter.convert(blockchain, it.address)),
                    value = it.percentage.multiply(BigDecimal(100)).toInt()
                )
            },
            lazySupply = BigInteger.ZERO,
            deleted = deleted,
            supply = if (deleted) BigInteger.ZERO else BigInteger.ONE,
            mintedAt = asset.createdAt ?: nowMillis(),
            lastUpdatedAt = asset.updatedAt ?: nowMillis()
        )
    }
}