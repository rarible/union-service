package com.rarible.protocol.union.search.indexer.repository

import com.ninjasquad.springmockk.MockkBean
import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.CurrencyRate
import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsItemSort
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.repository.search.internal.EsItemQueryScoreService
import com.rarible.protocol.union.enrichment.repository.search.internal.EsItemQuerySortService
import com.rarible.protocol.union.enrichment.repository.search.internal.mustMatchTerms
import com.rarible.protocol.union.enrichment.test.data.randomEsItem
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.elasticsearch.index.query.BoolQueryBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.test.context.ContextConfiguration
import java.math.BigDecimal

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
class EsItemQueryScoreServiceIntegrationTest {

    @MockkBean
    private lateinit var currencyService: CurrencyService

    @Autowired
    protected lateinit var repository: EsItemRepository

    @Autowired
    private lateinit var service: EsItemQueryScoreService

    // Score service should be tested in conjunction with sort service
    @Autowired
    private lateinit var sortService: EsItemQuerySortService

    @Autowired
    private lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        elasticsearchTestBootstrapper.bootstrap()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `should build score function by sell price`(descending: Boolean) = runBlocking<Unit> {
        // given
        val sort = if (descending) EsItemSort.HIGHEST_SELL_PRICE_FIRST else EsItemSort.LOWEST_SELL_PRICE_FIRST
        val builder = NativeSearchQueryBuilder()
        val first = randomEsItem().copy(
            blockchain = BlockchainDto.ETHEREUM,
            bestSellAmount = 10.0,
            bestSellCurrency = "ETHEREUM:0x0000000000000000000000000000000000000000",
            lastUpdatedAt = nowMillis().minusSeconds(45),
        )
        val second = randomEsItem().copy(
            blockchain = BlockchainDto.POLYGON,
            bestSellAmount = 20.0,
            bestSellCurrency = "POLYGON:0x0000000000000000000000000000000000000000",
            lastUpdatedAt = nowMillis().minusSeconds(55),
        )
        val third = randomEsItem().copy(
            blockchain = BlockchainDto.ETHEREUM,
            bestSellAmount = 100.0,
            bestSellCurrency = "ETHEREUM:0xfca59cd816ab1ead66534d82bc21e7515ce441cf",
            lastUpdatedAt = nowMillis().minusSeconds(35),
        )
        val fourth = randomEsItem().copy(
            blockchain = BlockchainDto.ETHEREUM,
            bestSellAmount = 99999.0,
            bestSellCurrency = "POLYGON:some-unknown-currency",
            lastUpdatedAt = nowMillis().minusSeconds(15),
        )
        val skipFirst = randomEsItem().copy(
            blockchain = BlockchainDto.FLOW,
            bestSellAmount = 100.0,
            bestSellCurrency = "FLOW:0xfca59cd816ab1ead66534d82bc21e7515ce441cf",
            lastUpdatedAt = nowMillis().minusSeconds(25),
        )
        val skipSecond = randomEsItem().copy(
            blockchain = BlockchainDto.SOLANA,
            bestSellAmount = 100.0,
            bestSellCurrency = "SOLANA:So11111111111111111111111111111111111111112",
            lastUpdatedAt = nowMillis().minusSeconds(35),
        )
        var expected = listOf(first, second, third, fourth)
        if (!descending) expected = expected.reversed()

        repository.saveAll(listOf(first, second, third, fourth, skipFirst, skipSecond))

        coEvery {
            currencyService.getAllCurrencyRates()
        } returns listOf(
            CurrencyRate(BlockchainDto.ETHEREUM, "ETHEREUM:0x0000000000000000000000000000000000000000", BigDecimal(1500)),
            CurrencyRate(BlockchainDto.POLYGON, "POLYGON:0x0000000000000000000000000000000000000000", BigDecimal(150)),
            CurrencyRate(BlockchainDto.POLYGON,"ETHEREUM:0xfca59cd816ab1ead66534d82bc21e7515ce441cf", BigDecimal(15)),
            CurrencyRate(BlockchainDto.SOLANA,"SOLANA:So11111111111111111111111111111111111111112", BigDecimal(50)),
            )
        val boolQueryBuilder = BoolQueryBuilder()
        val blockchains = setOf(BlockchainDto.ETHEREUM, BlockchainDto.POLYGON)
        boolQueryBuilder.mustMatchTerms(blockchains, EsItem::blockchain.name)

        // when
        builder.withQuery(service.buildQuery(boolQueryBuilder, sort, blockchains))
        sortService.applySort(builder, sort)
        val actual = repository.search(builder.build())
            .map { it.content }


        // then
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `should build score function by bid price`(descending: Boolean) = runBlocking<Unit> {
        // given
        val sort = if (descending) EsItemSort.HIGHEST_BID_PRICE_FIRST else EsItemSort.LOWEST_BID_PRICE_FIRST
        val builder = NativeSearchQueryBuilder()
        val first = randomEsItem().copy(
            blockchain = BlockchainDto.ETHEREUM,
            bestBidAmount = 10.0,
            bestBidCurrency = "ETHEREUM:0x0000000000000000000000000000000000000000",
            lastUpdatedAt = nowMillis().minusSeconds(45),
        )
        val second = randomEsItem().copy(
            blockchain = BlockchainDto.POLYGON,
            bestBidAmount = 20.0,
            bestBidCurrency = "POLYGON:0x0000000000000000000000000000000000000000",
            lastUpdatedAt = nowMillis().minusSeconds(55),
        )
        val third = randomEsItem().copy(
            blockchain = BlockchainDto.ETHEREUM,
            bestBidAmount = 100.0,
            bestBidCurrency = "ETHEREUM:0xfca59cd816ab1ead66534d82bc21e7515ce441cf",
            lastUpdatedAt = nowMillis().minusSeconds(35),
        )
        val fourth = randomEsItem().copy(
            blockchain = BlockchainDto.ETHEREUM,
            bestBidAmount = 99999.0,
            bestBidCurrency = "POLYGON:some-unknown-currency",
            lastUpdatedAt = nowMillis().minusSeconds(15),
        )
        val skipFirst = randomEsItem().copy(
            blockchain = BlockchainDto.FLOW,
            bestBidAmount = 100.0,
            bestBidCurrency = "FLOW:0xfca59cd816ab1ead66534d82bc21e7515ce441cf",
            lastUpdatedAt = nowMillis().minusSeconds(25),
        )
        val skipSecond = randomEsItem().copy(
            blockchain = BlockchainDto.SOLANA,
            bestBidAmount = 100.0,
            bestBidCurrency = "SOLANA:So11111111111111111111111111111111111111112",
            lastUpdatedAt = nowMillis().minusSeconds(35),
        )
        var expected = listOf(first, second, third, fourth)
        if (!descending) expected = expected.reversed()

        repository.saveAll(listOf(first, second, third, fourth, skipFirst, skipSecond))

        coEvery {
            currencyService.getAllCurrencyRates()
        } returns listOf(
            CurrencyRate(BlockchainDto.ETHEREUM, "ETHEREUM:0x0000000000000000000000000000000000000000", BigDecimal(1500)),
            CurrencyRate(BlockchainDto.POLYGON, "POLYGON:0x0000000000000000000000000000000000000000", BigDecimal(150)),
            CurrencyRate(BlockchainDto.POLYGON,"ETHEREUM:0xfca59cd816ab1ead66534d82bc21e7515ce441cf", BigDecimal(15)),
            CurrencyRate(BlockchainDto.SOLANA,"SOLANA:So11111111111111111111111111111111111111112", BigDecimal(50)),
        )
        val boolQueryBuilder = BoolQueryBuilder()
        val blockchains = setOf(BlockchainDto.ETHEREUM, BlockchainDto.POLYGON)
        boolQueryBuilder.mustMatchTerms(blockchains, EsItem::blockchain.name)

        // when
        builder.withQuery(service.buildQuery(boolQueryBuilder, sort, blockchains))
        sortService.applySort(builder, sort)
        val actual = repository.search(builder.build())
            .map { it.content }

        // then
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `should throw exception when scoring with LATEST_FIRST sort`() {
        // given
        coEvery { currencyService.getAllCurrencyRates() } returns emptyList()
        // when, then
        assertThatCode {
            runBlocking { service.buildQuery(BoolQueryBuilder(), EsItemSort.LATEST_FIRST, setOf(BlockchainDto.ETHEREUM)) }
        }.isExactlyInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun `should throw exception when scoring with EARLIEST_FIRST sort`() {
        // given
        coEvery { currencyService.getAllCurrencyRates() } returns emptyList()
        // when, then
        assertThatCode {
            runBlocking { service.buildQuery(BoolQueryBuilder(), EsItemSort.EARLIEST_FIRST, setOf(BlockchainDto.ETHEREUM)) }
        }.isExactlyInstanceOf(UnsupportedOperationException::class.java)
    }
}
