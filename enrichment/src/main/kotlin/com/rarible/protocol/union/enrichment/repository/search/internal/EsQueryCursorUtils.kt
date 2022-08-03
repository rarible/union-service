package com.rarible.protocol.union.enrichment.repository.search.internal

import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.RangeQueryBuilder
import org.elasticsearch.index.query.TermQueryBuilder
import java.time.Instant

fun BoolQueryBuilder.shouldAll(vararg queryBuilders: () -> QueryBuilder?) {
    val mustAll = BoolQueryBuilder()
    queryBuilders.forEach {
        val query = it()
        if (query != null) {
            mustAll.must(query)
        }
    }
    this.should(mustAll)
}

fun mustEqual(fieldName: String, value: Long): QueryBuilder? {
    if (value == 0L) return null
    return TermQueryBuilder(fieldName, value)
}

fun mustEqual(fieldName: String, value: Int): QueryBuilder? {
    if (value == 0) return null
    return TermQueryBuilder(fieldName, value)
}

fun mustEqual(fieldName: String, value: Double): QueryBuilder? {
    return TermQueryBuilder(fieldName, value)
}

fun mustEqual(fieldName: String, value: Instant?): QueryBuilder? {
    if (value == null) return null
    return TermQueryBuilder(fieldName, value)
}

fun mustDiffer(fieldName: String, value: Any?, descending: Boolean): QueryBuilder? {
    return if (value != null) {
        val rangeQuery = RangeQueryBuilder(fieldName)
        if (descending) {
            rangeQuery.lt(value)
        } else {
            rangeQuery.gt(value)
        }
        rangeQuery
    } else null
}
