package com.rarible.protocol.union.enrichment.repository.search

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.core.model.EsOwnershipFilter
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.enrichment.repository.search.internal.EsOwnershipQueryBuilderService
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter
import org.springframework.stereotype.Component

@Component
class EsOwnershipRepository(
    private val queryBuilderService: EsOwnershipQueryBuilderService,
    objectMapper: ObjectMapper,
    elasticsearchConverter: ElasticsearchConverter,
    esOperations: ReactiveElasticsearchOperations,
    elasticClient: ReactiveElasticsearchClient,
    esNameResolver: EsNameResolver
) : ElasticSearchRepository<EsOwnership>(
    objectMapper,
    esOperations,
    esNameResolver.createEntityDefinitionExtended(EsOwnership.ENTITY_DEFINITION),
    elasticsearchConverter,
    elasticClient,
    EsOwnership::class.java,
    EsOwnership::ownershipId.name,
) {

    override fun entityId(entity: EsOwnership): String {
        return entity.ownershipId
    }

    suspend fun search(filter: EsOwnershipFilter, limit: Int? = null): List<EsOwnership> {
        val query = queryBuilderService.build(filter)
        query.maxResults = PageSize.OWNERSHIP.limit(limit)
        query.trackTotalHits = false

        return esOperations.search(query, EsOwnership::class.java, entityDefinition.searchIndexCoordinates)
            .collectList().awaitFirst().map { it.content }
    }

}
