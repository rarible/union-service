package com.rarible.protocol.union.search.indexer.test

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.ext.KafkaTestExtension
import com.rarible.protocol.flow.nft.api.client.FlowNftItemControllerApi
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.converter.EsActivityConverter
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.UnionEventTopicProvider
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.repository.search.EsOrderRepository
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.search.indexer.config.IndexerProperties
import com.rarible.protocol.union.search.indexer.handler.ActivityEventHandler
import com.rarible.protocol.union.search.indexer.handler.CollectionEventHandler
import com.rarible.protocol.union.search.indexer.handler.ItemEventHandler
import com.rarible.protocol.union.search.indexer.handler.OrderEventHandler
import com.rarible.protocol.union.search.indexer.handler.OwnershipEventHandler
import com.rarible.protocol.union.search.indexer.metrics.IndexerMetricFactory
import com.rarible.protocol.union.search.indexer.metrics.MetricConsumerBatchEventHandlerFactory
import com.rarible.protocol.union.subscriber.UnionKafkaJsonSerializer
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.mockk
import org.elasticsearch.action.support.WriteRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary

@Lazy
@Configuration
class TestIndexerConfiguration {
    @Bean
    fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
        return ApplicationEnvironmentInfo("test", "test.com")
    }

    @Bean
    fun meterRegistry(): MeterRegistry {
        return SimpleMeterRegistry()
    }

    @Bean
    fun indexerMetricFactory(meterRegistry: MeterRegistry): IndexerMetricFactory {
        return IndexerMetricFactory(meterRegistry, IndexerProperties())
    }

    @Bean
    fun metricConsumerFactory(indexerMetricFactory: IndexerMetricFactory): MetricConsumerBatchEventHandlerFactory {
        return MetricConsumerBatchEventHandlerFactory(indexerMetricFactory)
    }

    @Bean
    fun activityHandler(
        featureFlagsProperties: FeatureFlagsProperties,
        repository: EsActivityRepository,
        converter: EsActivityConverter,
        indexerMetricFactory: IndexerMetricFactory
    ): ConsumerBatchEventHandler<ActivityDto> {
        return ActivityEventHandler(featureFlagsProperties, repository, converter, indexerMetricFactory)
    }

    @Bean
    fun orderHandler(repository: EsOrderRepository): ConsumerBatchEventHandler<OrderEventDto> {
        return OrderEventHandler(FeatureFlagsProperties(orderRefreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE), repository)
    }

    @Bean
    fun collectionHandler(
        repository: EsCollectionRepository,
        indexerMetricFactory: IndexerMetricFactory
    ): ConsumerBatchEventHandler<CollectionEventDto> {
        return CollectionEventHandler(FeatureFlagsProperties(), repository, indexerMetricFactory)
    }

    @Bean
    fun ownershipHandler(repository: EsOwnershipRepository): ConsumerBatchEventHandler<OwnershipEventDto> {
        return OwnershipEventHandler(FeatureFlagsProperties(), repository)
    }

    @Bean
    fun itemHandler(repository: EsItemRepository): ConsumerBatchEventHandler<ItemEventDto> {
        return ItemEventHandler(FeatureFlagsProperties(), repository)
    }

    //---------------- UNION producers ----------------//

    @Bean
    @Primary
    fun testUnionActivityEventProducer(): RaribleKafkaProducer<ActivityDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.activity",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = ActivityDto::class.java,
            defaultTopic = UnionEventTopicProvider.getActivityTopic(applicationEnvironmentInfo().name),
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    @Primary
    fun testUnionOrderEventProducer(): RaribleKafkaProducer<OrderEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.order",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = OrderEventDto::class.java,
            defaultTopic = UnionEventTopicProvider.getOrderTopic(applicationEnvironmentInfo().name),
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    @Primary
    fun testUnionCollectionEventProducer(): RaribleKafkaProducer<CollectionEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.collection",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = CollectionEventDto::class.java,
            defaultTopic = UnionEventTopicProvider.getCollectionTopic(applicationEnvironmentInfo().name),
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    @Primary
    fun testUnionItemEventProducer(): RaribleKafkaProducer<ItemEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.item",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = ItemEventDto::class.java,
            defaultTopic = UnionEventTopicProvider.getItemTopic(applicationEnvironmentInfo().name),
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    @Primary
    fun testUnionOwnershipEventProducer(): RaribleKafkaProducer<OwnershipEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.ownership",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = OwnershipEventDto::class.java,
            defaultTopic = UnionEventTopicProvider.getOwnershipTopic(applicationEnvironmentInfo().name),
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers()
        )
    }

    // --- APIs ---

    @Bean
    @Primary
    fun testFlowItemApi(): FlowNftItemControllerApi = mockk()

    @Bean("ethereum.item.api")
    @Primary
    fun testEthereumItemApi(): NftItemControllerApi = mockk()
}
