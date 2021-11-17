package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.protocol.tezos.dto.FTAssetTypeDto
import com.rarible.protocol.tezos.dto.MTAssetTypeDto
import com.rarible.protocol.tezos.dto.NFTAssetTypeDto
import com.rarible.protocol.tezos.dto.PartDto
import com.rarible.protocol.tezos.dto.XTZAssetTypeDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.TezosFTAssetTypeDto
import com.rarible.protocol.union.dto.TezosMTAssetTypeDto
import com.rarible.protocol.union.dto.TezosNFTAssetTypeDto
import com.rarible.protocol.union.dto.TezosXTZAssetTypeDto

object TezosConverter {

    fun convert(source: ActivitySortDto): com.rarible.protocol.tezos.dto.ActivitySortDto {
        return when (source) {
            ActivitySortDto.LATEST_FIRST -> com.rarible.protocol.tezos.dto.ActivitySortDto.LATEST_FIRST
            ActivitySortDto.EARLIEST_FIRST -> com.rarible.protocol.tezos.dto.ActivitySortDto.EARLIEST_FIRST
        }
    }

    fun convert(source: com.rarible.protocol.tezos.dto.AssetDto, blockchain: BlockchainDto): AssetDto {
        return AssetDto(
            type = convert(source = source.assetType, blockchain = blockchain),
            value = source.value
        )
    }

    fun convert(source: com.rarible.protocol.tezos.dto.AssetTypeDto, blockchain: BlockchainDto): AssetTypeDto {
        return when (source) {
            is XTZAssetTypeDto ->
                TezosXTZAssetTypeDto()
            is FTAssetTypeDto ->
                TezosFTAssetTypeDto(
                    contract = ContractAddress(blockchain, source.contract)
                )
            is NFTAssetTypeDto ->
                TezosNFTAssetTypeDto(
                    contract = ContractAddress(blockchain, source.contract),
                    tokenId = source.tokenId
                )
            is MTAssetTypeDto ->
                TezosMTAssetTypeDto(
                    contract = ContractAddress(blockchain, source.contract),
                    tokenId = source.tokenId
                )
        }
    }

    fun convertToCreator(
        source: PartDto,
        blockchain: BlockchainDto
    ): CreatorDto {
        return CreatorDto(
            account = UnionAddressConverter.convert(blockchain, source.account),
            value = source.value
        )
    }

}