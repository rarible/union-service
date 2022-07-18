package com.rarible.protocol.union.core.converter

import com.rarible.protocol.currency.dto.CurrencyDto
import com.rarible.protocol.currency.dto.CurrencyRateDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CurrencyIdDto
import com.rarible.protocol.union.dto.CurrencyUsdRateDto
import java.math.BigInteger

object CurrencyConverter {

    fun convert(source: CurrencyRateDto): CurrencyUsdRateDto {
        return CurrencyUsdRateDto(
            currencyId = source.fromCurrencyId,
            symbol = source.fromCurrencyId,
            rate = source.rate,
            date = source.date
        )
    }

    fun convert(source: CurrencyDto): com.rarible.protocol.union.dto.CurrencyDto {
        val pair = source.address.split(":")
        val address = pair[0]
        // Some currencies have tokenId (in Tezos, for example)
        val tokenId = pair.getOrNull(1)?.let { BigInteger(it) }
        return com.rarible.protocol.union.dto.CurrencyDto(
            currencyId = CurrencyIdDto(convert(source.blockchain), address, tokenId),
            symbol = source.currencyId,
            alias = source.alias
        )
    }

    fun convert(blockchain: BlockchainDto): com.rarible.protocol.currency.dto.BlockchainDto {
        return when (blockchain) {
            BlockchainDto.ETHEREUM, BlockchainDto.IMMUTABLEX -> com.rarible.protocol.currency.dto.BlockchainDto.ETHEREUM
            BlockchainDto.FLOW -> com.rarible.protocol.currency.dto.BlockchainDto.FLOW
            BlockchainDto.POLYGON -> com.rarible.protocol.currency.dto.BlockchainDto.POLYGON
            BlockchainDto.TEZOS -> com.rarible.protocol.currency.dto.BlockchainDto.TEZOS
            BlockchainDto.SOLANA -> com.rarible.protocol.currency.dto.BlockchainDto.SOLANA
            BlockchainDto.APTOS -> com.rarible.protocol.currency.dto.BlockchainDto.APTOS
        }
    }

    fun convert(blockchain: com.rarible.protocol.currency.dto.BlockchainDto): BlockchainDto {
        return when (blockchain) {
            com.rarible.protocol.currency.dto.BlockchainDto.ETHEREUM -> BlockchainDto.ETHEREUM
            com.rarible.protocol.currency.dto.BlockchainDto.FLOW -> BlockchainDto.FLOW
            com.rarible.protocol.currency.dto.BlockchainDto.POLYGON -> BlockchainDto.POLYGON
            com.rarible.protocol.currency.dto.BlockchainDto.TEZOS -> BlockchainDto.TEZOS
            com.rarible.protocol.currency.dto.BlockchainDto.SOLANA -> BlockchainDto.SOLANA
            com.rarible.protocol.currency.dto.BlockchainDto.APTOS -> BlockchainDto.APTOS
        }
    }
}
