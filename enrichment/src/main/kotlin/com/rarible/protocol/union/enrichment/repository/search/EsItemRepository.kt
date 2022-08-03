package com.rarible.protocol.union.enrichment.repository.search

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsItemFilter
import com.rarible.protocol.union.core.model.EsItemSort
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.enrichment.repository.search.internal.EsItemQueryBuilderService
import kotlinx.coroutines.reactive.awaitFirst
import org.elasticsearch.index.query.QueryBuilders.termQuery
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.stereotype.Component

@Component
class EsItemRepository(
    private val queryBuilderService: EsItemQueryBuilderService,
    objectMapper: ObjectMapper,
    elasticsearchConverter: ElasticsearchConverter,
    esOperations: ReactiveElasticsearchOperations,
    elasticClient: ReactiveElasticsearchClient,
    esNameResolver: EsNameResolver
) : ElasticSearchRepository<EsItem>(
    objectMapper,
    esOperations,
    esNameResolver.createEntityDefinitionExtended(EsItem.ENTITY_DEFINITION),
    elasticsearchConverter,
    elasticClient,
    EsItem::class.java,
    EsItem::itemId.name,
) {

    override fun entityId(entity: EsItem): String {
        return entity.itemId
    }

    suspend fun search(filter: EsItemFilter, sort: EsItemSort, limit: Int?): List<EsItem> {
        val query = queryBuilderService.build(filter, sort)

        query.maxResults = PageSize.ITEM.limit(limit)
        query.trackTotalHits = false

        return search(query)
    }

    // TODO: return lightweight EsItem type (similarly to EsActivityLite)
    suspend fun search(query: NativeSearchQuery): List<EsItem> {
        return esOperations.search(query, EsItem::class.java, entityDefinition.searchIndexCoordinates)
            .collectList()
            .awaitFirst()
            .map { it.content }
    }

    suspend fun countItemsInCollection(collectionId: String): Long {
        val query = NativeSearchQuery(termQuery(EsItem::collection.name, collectionId))

        return esOperations
            .count(
                query,
                EsItem::class.java,
                entityDefinition.searchIndexCoordinates
            )
            .awaitFirst()
    }
}
