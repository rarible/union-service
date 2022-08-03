package com.rarible.protocol.union.core.es

import com.rarible.protocol.union.core.elasticsearch.EsMetadataRepository
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.elasticsearch.EsRepository
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations

@Import(EsMetadataRepository::class)
class ElasticsearchBootstrapperTestConfig {

    @Bean(initMethod = "bootstrap")
    fun elasticsearchBootstrap(
        reactiveElasticSearchOperations: ReactiveElasticsearchOperations,
        restHighLevelClient: ReactiveElasticsearchClient,
        esNameResolver: EsNameResolver,
        indexService: IndexService,
        repositories: List<EsRepository>
    ): ElasticsearchTestBootstrapper {

        return ElasticsearchTestBootstrapper(
            esNameResolver = esNameResolver,
            esOperations = reactiveElasticSearchOperations,
            restHighLevelClient = restHighLevelClient,
            entityDefinitions = EsEntitiesConfig.createEsEntities(),
            repositories = repositories
        )
    }
}