package com.rarible.protocol.union.search.indexer.repository

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.EsItemSort
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.repository.search.internal.EsItemQuerySortService
import com.rarible.protocol.union.enrichment.test.data.randomEsItem
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.test.context.ContextConfiguration

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
class EsItemQuerySortServiceIntegrationTest {

    @Autowired
    protected lateinit var repository: EsItemRepository

    // Sort by price is tested in EsItemQueryScoreServiceIntegrationTest
    @Autowired
    private lateinit var service: EsItemQuerySortService

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `should sort by lastUpdatedAt and id`(descending: Boolean) = runBlocking<Unit> {
        // given
        val builder = NativeSearchQueryBuilder()
        val sort = if (descending) EsItemSort.LATEST_FIRST else EsItemSort.EARLIEST_FIRST
        val first = randomEsItem().copy(lastUpdatedAt = nowMillis().plusSeconds(100), itemId = "A")
        val second = randomEsItem().copy(lastUpdatedAt = nowMillis().plusSeconds(100), itemId = "B")
        val third = randomEsItem().copy(lastUpdatedAt = nowMillis().plusSeconds(50), itemId = "C")
        val fourth = randomEsItem().copy(lastUpdatedAt = nowMillis().plusSeconds(25), itemId = "D")
        val fifth = randomEsItem().copy(lastUpdatedAt = nowMillis().plusSeconds(25), itemId = "E")
        repository.saveAll(listOf(first, second, third, fourth, fifth).shuffled())
        val expected = if (descending) {
            listOf(first, second, third, fourth, fifth)
        } else {
            listOf(fourth, fifth, third, first, second)
        }

        // when
        service.applySort(builder, sort)
        val actual = repository.search(builder.build())
            .map { it.content }

        // then
        assertThat(actual).isEqualTo(expected)
    }
}
