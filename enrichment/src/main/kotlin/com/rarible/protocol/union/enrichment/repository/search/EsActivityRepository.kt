package com.rarible.protocol.union.enrichment.repository.search

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.model.ElasticActivityFilter
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.EsActivityCursor.Companion.fromActivityLite
import com.rarible.protocol.union.core.model.EsActivityLite
import com.rarible.protocol.union.core.model.EsActivityQueryResult
import com.rarible.protocol.union.core.model.EsActivitySort
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.enrichment.repository.search.internal.EsActivityQueryBuilderService
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.stereotype.Component

@Component
class EsActivityRepository(
    private val queryBuilderService: EsActivityQueryBuilderService,
    objectMapper: ObjectMapper,
    elasticsearchConverter: ElasticsearchConverter,
    esOperations: ReactiveElasticsearchOperations,
    elasticClient: ReactiveElasticsearchClient,
    esNameResolver: EsNameResolver
) : ElasticSearchRepository<EsActivity>(
    objectMapper,
    esOperations,
    esNameResolver.createEntityDefinitionExtended(EsActivity.ENTITY_DEFINITION),
    elasticsearchConverter,
    elasticClient,
    EsActivity::class.java,
    EsActivity::activityId.name,
) {

    override fun entityId(entity: EsActivity): String {
        return entity.activityId
    }

    suspend fun search(
        filter: ElasticActivityFilter,
        sort: EsActivitySort,
        limit: Int?
    ): EsActivityQueryResult {
        val query = queryBuilderService.build(filter, sort)
        query.maxResults = PageSize.ACTIVITY.limit(limit)
        query.trackTotalHits = false

        return search(query)
    }

    suspend fun search(query: NativeSearchQuery): EsActivityQueryResult {
        val activities = esOperations.search(query, EsActivityLite::class.java, entityDefinition.searchIndexCoordinates)
            .collectList()
            .awaitFirst()
            .map { it.content }

        val cursor = if (activities.isEmpty()) {
            null
        } else {
            activities.last().fromActivityLite().toString()
        }

        return EsActivityQueryResult(
            activities = activities,
            cursor = cursor
        )
    }
}
