package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.tezos.api.client.NftOwnershipControllerApi
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.tezos.converter.TezosOwnershipConverter
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktOwnershipService
import kotlinx.coroutines.reactive.awaitFirst

@CaptureSpan(type = "blockchain")
open class TezosOwnershipService(
    private val ownershipControllerApi: NftOwnershipControllerApi,
    private val tzktOwnershipService: TzktOwnershipService
) : AbstractBlockchainService(BlockchainDto.TEZOS), OwnershipService {

    override suspend fun getOwnershipById(ownershipId: String): UnionOwnership {
        return if (tzktOwnershipService.enabled()) {
            tzktOwnershipService.getOwnershipById(ownershipId)
        } else {
            val ownership = ownershipControllerApi.getNftOwnershipById(ownershipId).awaitFirst()
            return TezosOwnershipConverter.convert(ownership, blockchain)
        }
    }

    override suspend fun getOwnershipsByItem(
        itemId: String,
        continuation: String?,
        size: Int
    ): Page<UnionOwnership> {
        return if (tzktOwnershipService.enabled()) {
            tzktOwnershipService.getOwnershipsByItem(itemId, continuation, size)
        } else {
            val (contract, tokenId) = CompositeItemIdParser.split(itemId)
            val ownerships = ownershipControllerApi.getNftOwnershipByItem(
                contract,
                tokenId.toString(),
                size,
                continuation
            ).awaitFirst()
            return TezosOwnershipConverter.convert(ownerships, blockchain)
        }
    }

    override suspend fun getOwnershipsByOwner(address: String, continuation: String?, size: Int): Page<UnionOwnership> {
        // Will be implemented via es
        return Page.empty()
    }
}
