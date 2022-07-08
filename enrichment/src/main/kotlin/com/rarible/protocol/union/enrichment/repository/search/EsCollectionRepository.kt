package com.rarible.protocol.union.enrichment.repository.search

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.model.EsCollection
import com.rarible.protocol.union.core.model.EsCollectionFilter
import com.rarible.protocol.union.core.model.EsCollectionLite
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.enrichment.repository.search.internal.EsCollectionQueryBuilderService
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.stereotype.Component

@Component
class EsCollectionRepository(
    private val queryBuilderService: EsCollectionQueryBuilderService,
    objectMapper: ObjectMapper,
    elasticsearchConverter: ElasticsearchConverter,
    esOperations: ReactiveElasticsearchOperations,
    elasticClient: ReactiveElasticsearchClient,
    esNameResolver: EsNameResolver
) : ElasticSearchRepository<EsCollection>(
    objectMapper,
    esOperations,
    esNameResolver.createEntityDefinitionExtended(EsCollection.ENTITY_DEFINITION),
    elasticsearchConverter,
    elasticClient,
    EsCollection::class.java,
    EsCollection::collectionId.name,
) {

    override fun entityId(entity: EsCollection): String {
        return entity.collectionId
    }

    suspend fun search(filter: EsCollectionFilter, limit: Int?): List<EsCollectionLite> {
        val query = queryBuilderService.build(filter)
        query.maxResults = PageSize.COLLECTION.limit(limit)
        query.trackTotalHits = false

        return search(query)
    }

    suspend fun search(query: NativeSearchQuery): List<EsCollectionLite> {
        return esOperations.search(query, EsCollectionLite::class.java, entityDefinition.searchIndexCoordinates)
            .collectList()
            .awaitFirst()
            .map { it.content }
    }
}
