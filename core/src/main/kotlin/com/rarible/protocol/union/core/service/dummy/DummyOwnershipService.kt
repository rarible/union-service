package com.rarible.protocol.union.core.service.dummy

import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Slice

class DummyOwnershipService(
    blockchain: BlockchainDto
) : AbstractBlockchainService(blockchain), OwnershipService {

    override suspend fun getOwnershipById(ownershipId: String): UnionOwnership {
        throw UnionNotFoundException("Ownership [$ownershipId] not found, ${blockchain.name} is not available")
    }

    override suspend fun getOwnershipsByIds(ownershipIds: List<String>): List<UnionOwnership> =
        emptyList()

    override suspend fun getOwnershipsAll(continuation: String?, size: Int): Slice<UnionOwnership> =
        Slice.empty()

    override suspend fun getOwnershipsByItem(
        itemId: String,
        continuation: String?,
        size: Int,
    ): Page<UnionOwnership> {
        return Page.empty()
    }

    override suspend fun getOwnershipsByOwner(address: String, continuation: String?, size: Int): Page<UnionOwnership> {
        return Page.empty()
    }
}
