package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsItemSort
import org.springframework.data.elasticsearch.core.SearchHit
import org.springframework.stereotype.Component

@Component
class EsItemQueryCursorService {

    fun buildSearchAfterClause(cursorAsString: String?): List<Any>? {
        if (cursorAsString.isNullOrEmpty()) return null
        return cursorAsString.split("_")
    }

    fun buildCursor(lastSearchHit: SearchHit<EsItem>?): String? {
        if (lastSearchHit == null) return null
        return lastSearchHit.sortValues.joinToString("_")
    }
}
