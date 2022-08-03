package com.rarible.protocol.union.enrichment.repository.search

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.model.EsOrder
import com.rarible.protocol.union.core.model.EsOrderFilter
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter
import org.springframework.stereotype.Component

@Component
class EsOrderRepository(
    objectMapper: ObjectMapper,
    elasticsearchConverter: ElasticsearchConverter,
    esOperations: ReactiveElasticsearchOperations,
    elasticClient: ReactiveElasticsearchClient,
    esNameResolver: EsNameResolver
) : ElasticSearchRepository<EsOrder>(
    objectMapper,
    esOperations,
    esNameResolver.createEntityDefinitionExtended(EsOrder.ENTITY_DEFINITION),
    elasticsearchConverter,
    elasticClient,
    EsOrder::class.java,
    EsOrder::orderId.name,
) {

    override fun entityId(entity: EsOrder): String {
        return entity.orderId
    }

    suspend fun findByFilter(filter: EsOrderFilter): List<EsOrder> {
        val query = filter.asQuery()
        return esOperations.search(query, EsOrder::class.java, entityDefinition.searchIndexCoordinates)
            .collectList().awaitFirst().map { it.content }
    }
}
