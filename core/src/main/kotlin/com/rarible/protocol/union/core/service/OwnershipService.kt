package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.router.BlockchainService
import com.rarible.protocol.union.dto.continuation.page.Page

interface OwnershipService : BlockchainService {

    suspend fun getOwnershipById(
        ownershipId: String
    ): UnionOwnership

    suspend fun getOwnershipsByItem(
        itemId: String,
        continuation: String?,
        size: Int
    ): Page<UnionOwnership>

    suspend fun getOwnershipsByOwner(
        address: String,
        continuation: String?,
        size: Int
    ): Page<UnionOwnership>

}
