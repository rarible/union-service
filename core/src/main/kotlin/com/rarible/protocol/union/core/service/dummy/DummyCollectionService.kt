package com.rarible.protocol.union.core.service.dummy

import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.TokenId
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Page

class DummyCollectionService(
    blockchain: BlockchainDto
) : AbstractBlockchainService(blockchain), CollectionService {

    override suspend fun getAllCollections(
        continuation: String?,
        size: Int
    ) : Page<UnionCollection> = Page.empty()

    override suspend fun getCollectionById(collectionId: String): UnionCollection {
        throw UnionNotFoundException("Collection [$collectionId] not found, ${blockchain.name} is not available")
    }

    override suspend fun refreshCollectionMeta(collectionId: String) {
        // Do nothing?
    }

    override suspend fun getCollectionsByIds(ids: List<String>): List<UnionCollection> {
        TODO("Not yet implemented")
    }

    override suspend fun generateNftTokenId(collectionId: String, minter: String?): TokenId {
        TODO("Not yet implemented")
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ): Page<UnionCollection> {
        return Page.empty()
    }
}
