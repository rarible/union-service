package com.rarible.protocol.union.integration.solana.converter

import com.rarible.protocol.solana.dto.TokenCreatorPartDto
import com.rarible.protocol.solana.dto.TokenDto
import com.rarible.protocol.solana.dto.TokensDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.continuation.page.Page
import java.math.BigInteger

object SolanaItemConverter {

    fun convert(token: TokenDto, blockchain: BlockchainDto): UnionItem {
        return UnionItem(
            id = ItemIdDto(
                blockchain = blockchain,
                value = token.address
            ),
            creators = token.creators.orEmpty().map { convert(it, blockchain) },
            // TODO it MUST be not-null
            collection = token.collection?.let { CollectionIdDto(blockchain, it) },
            lazySupply = BigInteger.ZERO,
            pending = emptyList(),
            mintedAt = token.createdAt,
            lastUpdatedAt = token.updatedAt,
            supply = token.supply,
            meta = null,
            deleted = token.closed
        )
    }

    fun convert(page: TokensDto, blockchain: BlockchainDto): Page<UnionItem> {
        return Page(
            total = 0,
            continuation = page.continuation,
            entities = page.tokens.map { convert(it, blockchain) }
        )
    }

    fun convert(source: com.rarible.protocol.solana.dto.RoyaltyDto, blockchain: BlockchainDto): RoyaltyDto {
        return RoyaltyDto(
            account = UnionAddressConverter.convert(blockchain, source.account),
            value = source.value
        )
    }

    private fun convert(creator: TokenCreatorPartDto, blockchain: BlockchainDto) =
        CreatorDto(
            account = UnionAddressConverter.convert(blockchain, creator.address),
            value = creator.share
        )
}
