package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.CurrencyRate
import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsItemSort
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.BlockchainDto
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders
import org.springframework.stereotype.Service

@Service
class EsItemQueryScoreService(
    private val currencyService: CurrencyService,
) {

    suspend fun buildQuery(
        boolQuery: BoolQueryBuilder,
        sort: EsItemSort,
        blockchains: Set<BlockchainDto>
    ): FunctionScoreQueryBuilder {
        val currencyRates = currencyService.getAllCurrencyRates()
        val functions = when (sort) {
            EsItemSort.LATEST_FIRST,
            EsItemSort.EARLIEST_FIRST -> throw UnsupportedOperationException("ScoreService can't be used with $sort")
            EsItemSort.HIGHEST_SELL_PRICE_FIRST,
            EsItemSort.LOWEST_SELL_PRICE_FIRST -> getPriceScoreFilterFunctions(
                sort == EsItemSort.HIGHEST_SELL_PRICE_FIRST,
                currencyRates,
                blockchains,
                EsItem::bestSellCurrency.name,
                EsItem::bestSellAmount.name,
            )
            EsItemSort.HIGHEST_BID_PRICE_FIRST,
            EsItemSort.LOWEST_BID_PRICE_FIRST -> getPriceScoreFilterFunctions(
                sort == EsItemSort.HIGHEST_BID_PRICE_FIRST,
                currencyRates,
                blockchains,
                EsItem::bestBidCurrency.name,
                EsItem::bestBidAmount.name,
            )
        }

        return QueryBuilders.functionScoreQuery(
            boolQuery,
            functions.toTypedArray()
        )
    }

    private fun getPriceScoreFilterFunctions(
        descSort: Boolean,
        currencyRates: List<CurrencyRate>,
        blockchains: Set<BlockchainDto>,
        currencyField: String,
        amountField: String,
    ): List<FunctionScoreQueryBuilder.FilterFunctionBuilder> {
        val missingFactor = if (descSort) 0.0f else Float.MAX_VALUE

        return currencyRates.filter { blockchains.contains(it.blockchain) }
            .map { currencyRate ->
                FunctionScoreQueryBuilder.FilterFunctionBuilder(
                    QueryBuilders.boolQuery().must(QueryBuilders.termQuery(currencyField, currencyRate.currencyId))
                        .must(QueryBuilders.existsQuery(amountField)),
                    ScoreFunctionBuilders.fieldValueFactorFunction(amountField).factor(currencyRate.rate.toFloat())
                )
            }.plusElement(
                FunctionScoreQueryBuilder.FilterFunctionBuilder(
                    QueryBuilders.boolQuery()
                        .should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(amountField)))
                        .should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(currencyField))),
                    ScoreFunctionBuilders.fieldValueFactorFunction(amountField).missing(1.0)
                        .factor(missingFactor)
                )
            )
    }
}
