package com.rarible.protocol.union.search.indexer.repository

import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.ElasticActivityQueryGenericFilter
import com.rarible.protocol.union.core.model.EsActivityCursor
import com.rarible.protocol.union.core.model.EsActivitySort
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.test.data.info
import com.rarible.protocol.union.enrichment.test.data.randomEsActivity
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.test.context.ContextConfiguration
import java.time.Instant

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
internal class EsActivityQueryServiceIntegrationTest {

    @Autowired
    protected lateinit var repository: EsActivityRepository

    @Autowired
    private lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        elasticsearchTestBootstrapper.bootstrap()
    }

    @Test
    fun `should return up to 3 activities from query`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(
            blockchains = setOf(BlockchainDto.SOLANA, BlockchainDto.TEZOS)
        )
        val toFind1 = randomEsActivity().copy(
            blockchain = BlockchainDto.SOLANA,
            date = Instant.ofEpochMilli(5000),
        )
        val toFind2 = randomEsActivity().copy(
            blockchain = BlockchainDto.TEZOS,
            date = Instant.ofEpochMilli(4000),
        )
        val toFind3 = randomEsActivity().copy(
            blockchain = BlockchainDto.SOLANA,
            date = Instant.ofEpochMilli(3000),
        )
        val toSkip1 = randomEsActivity().copy(
            blockchain = BlockchainDto.TEZOS,
            date = Instant.ofEpochMilli(2000),
        )
        val toSkip2 = randomEsActivity().copy(
            blockchain = BlockchainDto.SOLANA,
            date = Instant.ofEpochMilli(1000),
        )
        repository.saveAll(listOf(toSkip1, toFind3, toSkip2, toFind2, toFind1))
        repository.refresh()

        // when
        val actual = repository.search(filter, EsActivitySort(true), 3)

        // then
        assertThat(actual.activities).containsExactly(toFind1.info, toFind2.info, toFind3.info)
        val cursor = EsActivityCursor.fromString(actual.cursor!!)!!
        assertThat(cursor.date).isEqualTo(toFind3.date)
        assertThat(cursor.blockNumber).isEqualTo(toFind3.blockNumber)
        assertThat(cursor.logIndex).isEqualTo(toFind3.logIndex)
        assertThat(cursor.salt).isEqualTo(toFind3.salt)
    }

    @Test
    fun `should be able to iterate over activities with cursor`() = runBlocking<Unit> {
        // given
        val first = randomEsActivity().copy(date = Instant.ofEpochMilli(100))
        val second = randomEsActivity().copy(date = Instant.ofEpochMilli(200))
        val third = randomEsActivity().copy(date = Instant.ofEpochMilli(300))
        val fourth = randomEsActivity().copy(date = Instant.ofEpochMilli(400))
        val fifth = randomEsActivity().copy(date = Instant.ofEpochMilli(500))
        val sixth = randomEsActivity().copy(date = Instant.ofEpochMilli(600))
        repository.saveAll(listOf(second, fourth, sixth, fifth, third, first))
        var filter = ElasticActivityQueryGenericFilter()

        // when
        val query1 = repository.search(filter, EsActivitySort(false), 2)
        filter = filter.copy(cursor = query1.cursor)
        val query2 = repository.search(filter, EsActivitySort(false), 2)
        filter = filter.copy(cursor = query2.cursor)
        val query3 = repository.search(filter, EsActivitySort(false), 2)
        filter = filter.copy(cursor = query3.cursor)
        val query4 = repository.search(filter, EsActivitySort(false), 2)

        // then
        assertThat(query1.activities).containsExactly(first.info, second.info)
        assertThat(query2.activities).containsExactly(third.info, fourth.info)
        assertThat(query3.activities).containsExactly(fifth.info, sixth.info)
        assertThat(query4.activities).isEmpty()
        assertThat(query4.cursor).isNull()
    }
}
