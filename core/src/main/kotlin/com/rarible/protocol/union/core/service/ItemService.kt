package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.model.UnionMedia
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.router.BlockchainService
import com.rarible.protocol.union.dto.RoyaltyDto

interface ItemService : BlockchainService {

    suspend fun getAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): Page<UnionItem>

    suspend fun getItemById(
        itemId: String
    ): UnionItem

    suspend fun getItemRoyaltiesById(
        itemId: String
    ): List<RoyaltyDto>

    suspend fun getItemMetaById(
        itemId: String
    ): UnionMeta

    suspend fun getItemImageById(
        itemId: String
    ): UnionMedia

    suspend fun getItemAnimationById(
        itemId: String
    ): UnionMedia

    suspend fun resetItemMeta(
        itemId: String
    )

    suspend fun getItemsByCollection(
        collection: String,
        owner: String?,
        continuation: String?,
        size: Int
    ): Page<UnionItem>

    suspend fun getItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int
    ): Page<UnionItem>

    suspend fun getItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionItem>

}
