package com.rarible.protocol.union.enrichment.meta

import com.rarible.loader.cache.CacheLoader
import com.rarible.loader.cache.CacheType
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.parser.IdParser
import org.springframework.stereotype.Component

@Component
class UnionMetaCacheLoader(
    private val unionMetaLoader: UnionMetaLoader
) : CacheLoader<UnionMeta> {

    override val type
        get() = TYPE

    override suspend fun load(key: String): UnionMeta {
        val itemId = IdParser.parseItemId(key)
        return unionMetaLoader.load(itemId)
    }

    companion object {
        const val TYPE: CacheType = "union-meta"
    }
}