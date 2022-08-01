package com.rarible.protocol.union.enrichment.repository.search.internal

import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.RangeQueryBuilder
import org.elasticsearch.index.query.TermQueryBuilder
import org.elasticsearch.index.query.TermsQueryBuilder
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import java.time.Instant
import kotlin.reflect.KProperty

fun BoolQueryBuilder.mustMatchTerms(terms: Set<*>?, field: String) {
    if (terms != null && terms.isNotEmpty()) {
        must(TermsQueryBuilder(field, prepareTerms(terms)))
    }
}

fun BoolQueryBuilder.mustMatchTerm(term: String?, field: String) {
    if (!term.isNullOrEmpty()) {
        must(TermQueryBuilder(field, term))
    }
}

fun BoolQueryBuilder.mustMatchRange(from: Instant?, to: Instant?, field: String) {
    if (from != null || to != null) {
        val rangeQueryBuilder = RangeQueryBuilder(field)
        if (from != null) {
            rangeQueryBuilder.gte(from)
        }
        if (to != null) {
            rangeQueryBuilder.lte(to)
        }
        must(rangeQueryBuilder)
    }
}

fun prepareTerms(terms: Set<*>): List<String> {
    return terms.map { it.toString() }
}

fun NativeSearchQueryBuilder.sortByField(field: KProperty<*>, order: SortOrder) {
    val sort = SortBuilders
        .fieldSort(field.name)
        .order(order)
    withSort(sort)
}
