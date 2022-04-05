package com.rarible.protocol.union.enrichment.repository

import com.mongodb.client.result.DeleteResult
import com.rarible.protocol.union.enrichment.model.ShortCollection
import com.rarible.protocol.union.enrichment.model.ShortCollectionId

interface CollectionRepository {
    suspend fun createIndices()
    suspend fun save(collection: ShortCollection): ShortCollection
    suspend fun get(collectionId: ShortCollectionId): ShortCollection?
    suspend fun delete(collectionId: ShortCollectionId): DeleteResult?
    suspend fun getAll(): List<ShortCollection>
}