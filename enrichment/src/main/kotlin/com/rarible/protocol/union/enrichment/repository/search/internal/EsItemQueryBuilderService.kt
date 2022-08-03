package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsItemFilter
import com.rarible.protocol.union.core.model.EsItemGenericFilter
import com.rarible.protocol.union.core.model.EsItemSort
import com.rarible.protocol.union.dto.BlockchainDto
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.MultiMatchQueryBuilder
import org.elasticsearch.index.query.Operator
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.QueryBuilders.termsQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Component

@Component
class EsItemQueryBuilderService(
    private val cursorService: EsItemQueryCursorService,
    private val scoreService: EsItemQueryScoreService,
    private val sortService: EsItemQuerySortService,
) {
    companion object {
        private val SCORE_SORT_TYPES: Set<EsItemSort> = setOf(
            EsItemSort.LOWEST_SELL_PRICE_FIRST,
            EsItemSort.HIGHEST_SELL_PRICE_FIRST,
            EsItemSort.LOWEST_BID_PRICE_FIRST,
            EsItemSort.HIGHEST_BID_PRICE_FIRST
        )
    }

    suspend fun build(filter: EsItemFilter, sort: EsItemSort): NativeSearchQuery {
        val builder = NativeSearchQueryBuilder()
        val isScoreSort = SCORE_SORT_TYPES.contains(sort)

        var query: QueryBuilder = BoolQueryBuilder()
        query as BoolQueryBuilder
        when (filter) {
            is EsItemGenericFilter -> query.applyGenericFilter(filter)
        }

        if (isScoreSort) {
            val blockchains = if (filter.blockchains.isNullOrEmpty()) {
                BlockchainDto.values().toSet()
            } else {
                filter.blockchains!!.map { BlockchainDto.valueOf(it) }.toSet()
            }
            query = scoreService.buildQuery(query, sort, blockchains)
        }

        sortService.applySort(builder, sort)

        builder.withQuery(query)
        val resultQuery = builder.build()
        resultQuery.searchAfter = cursorService.buildSearchAfterClause(filter.cursor)
        return resultQuery
    }

    private fun BoolQueryBuilder.applyGenericFilter(filter: EsItemGenericFilter) {
        mustMatchTerms(filter.collections, EsItem::collection.name)
        mustMatchTerms(filter.creators, EsItem::creators.name)
        mustMatchTerms(filter.blockchains, EsItem::blockchain.name)
        mustMatchTerms(filter.itemIds, EsItem::itemId.name)
        mustMatchRange(filter.mintedFrom, filter.mintedTo, EsItem::mintedAt.name)
        mustMatchRange(filter.updatedFrom, filter.updatedTo, EsItem::lastUpdatedAt.name)
        applyTextFilter(filter.text)
        applyTraitsKeysFilter(filter.traitsKeys)
        applyTraitsValuesFilter(filter.traitsValues)
    }

    private fun BoolQueryBuilder.applyTextFilter(text: String?) {
        if (!text.isNullOrBlank()) {
            should(
                QueryBuilders.nestedQuery(
                    "traits",
                    QueryBuilders.boolQuery().must(QueryBuilders.termQuery("traits.value.raw", text)),
                    ScoreMode.None
                )
            )

            val trimmedText = text.trim()
            val lastTerm = trimmedText.split(" ").last()
            val textForSearch = if (lastTerm == trimmedText) {
                "($lastTerm | $lastTerm*)"
            } else {
                trimmedText.replaceAfterLast(" ", "($lastTerm | $lastTerm*)")
            }
            should(
                QueryBuilders.simpleQueryStringQuery(textForSearch)
                    .defaultOperator(Operator.AND)
                    .fuzzyTranspositions(false)
                    .fuzzyMaxExpansions(0)
                    .fields(mapOf(EsItem::name.name to 1.0f))
            )
                // phrase. boost = 100
                .should(
                    QueryBuilders.multiMatchQuery(text)
                        .fields(mapOf(EsItem::name.name to 1.0f))
                        .boost(100f)
                        .fuzzyTranspositions(false)
                        .operator(Operator.AND)
                        .type(MultiMatchQueryBuilder.Type.PHRASE)
                )

            minimumShouldMatch(1)
        }
    }

    private fun BoolQueryBuilder.applyTraitsKeysFilter(traitsKeys: Set<String>?) {
        if (traitsKeys != null) {
            should(
                QueryBuilders.nestedQuery(
                    "traits",
                    QueryBuilders.boolQuery().must(termsQuery("traits.key.raw", traitsKeys)),
                    ScoreMode.None
                )
            )
        }
    }

    private fun BoolQueryBuilder.applyTraitsValuesFilter(traitsValues: Set<String>?) {
        if (traitsValues != null) {
            should(
                QueryBuilders.nestedQuery(
                    "traits",
                    QueryBuilders.boolQuery().must(termsQuery("traits.value.raw", traitsValues)),
                    ScoreMode.None
                )
            )
        }
    }
}
