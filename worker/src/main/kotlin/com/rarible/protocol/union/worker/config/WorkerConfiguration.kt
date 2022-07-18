package com.rarible.protocol.union.worker.config

import com.rarible.core.task.EnableRaribleTask
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.elasticsearch.EsRepository
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.core.elasticsearch.bootstrap.ElasticsearchBootstrapper
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig
import com.rarible.protocol.union.enrichment.configuration.EnrichmentApiConfiguration
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.worker.task.search.ReindexService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations

@Configuration
@Import(
    value = [
        EnrichmentApiConfiguration::class,
        SearchConfiguration::class
    ]
)
@EnableRaribleTask
@EnableConfigurationProperties(WorkerProperties::class)
class WorkerConfiguration(
    val properties: WorkerProperties
) {
    @Bean
    fun activityReindexProperties(): ActivityReindexProperties {
        return properties.searchReindex.activity
    }

    @Bean
    fun collectionReindexProperties(): CollectionReindexProperties {
        return properties.searchReindex.collection
    }

    @Bean
    fun itemReindexProperties(): ItemReindexProperties {
        return properties.searchReindex.item
    }

    @Bean
    fun orderReindexProperties(): OrderReindexProperties {
        return properties.searchReindex.order
    }

    @Bean
    fun ownershipReindexProperties(): OwnershipReindexProperties {
        return properties.searchReindex.ownership
    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    @Bean(initMethod = "bootstrap")
    @Profile("!test")
    fun elasticsearchBootstrap(
        reactiveElasticSearchOperations: ReactiveElasticsearchOperations,
        esNameResolver: EsNameResolver,
        reindexerService: ReindexService,
        indexService: IndexService,
        esRepositories: List<EsRepository>,
        reindexService: ReindexService,
        highLevelClient: RestHighLevelClient,
    ): ElasticsearchBootstrapper {
        return ElasticsearchBootstrapper(
            esNameResolver = esNameResolver,
            esOperations = reactiveElasticSearchOperations,
            entityDefinitions = EsEntitiesConfig.prodEsEntities(),
            reindexSchedulingService = reindexService,
            indexService = indexService,
            repositories = esRepositories,
            restHighLevelClient = highLevelClient,
        )
    }
}