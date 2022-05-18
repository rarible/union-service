package com.rarible.protocol.union.search.indexer.test

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.ext.KafkaTestExtension
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.UnionEventTopicProvider
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.enrichment.repository.search.EsOrderRepository
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.search.indexer.config.IndexerProperties
import com.rarible.protocol.union.search.indexer.handler.ActivityEventHandler
import com.rarible.protocol.union.search.indexer.handler.CollectionEventHandler
import com.rarible.protocol.union.search.indexer.handler.OrderEventHandler
import com.rarible.protocol.union.search.indexer.handler.OwnershipEventHandler
import com.rarible.protocol.union.search.indexer.metrics.IndexerMetricFactory
import com.rarible.protocol.union.search.indexer.metrics.MetricConsumerBatchEventHandlerFactory
import com.rarible.protocol.union.subscriber.UnionKafkaJsonSerializer
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy

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
    fun activityHandler(repository: EsActivityRepository): ConsumerBatchEventHandler<ActivityDto> {
        return ActivityEventHandler(repository)
    }

    @Bean
    fun orderHandler(repository: EsOrderRepository): ConsumerBatchEventHandler<OrderEventDto> {
        return OrderEventHandler(repository)
    }

    @Bean
    fun collectionHandler(repository: EsCollectionRepository): ConsumerBatchEventHandler<CollectionEventDto> {
        return CollectionEventHandler(repository)
    }

    @Bean
    fun ownershipHandler(repository: EsOwnershipRepository): ConsumerBatchEventHandler<OwnershipEventDto> {
        return OwnershipEventHandler(repository)
    }

    //---------------- UNION producers ----------------//

    @Bean
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
    fun testUnionItemEventProducer(): RaribleKafkaProducer<ItemEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.item",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = ItemEventDto::class.java,
            defaultTopic = UnionEventTopicProvider.getCollectionTopic(applicationEnvironmentInfo().name),
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testUnionOwnershipEventProducer(): RaribleKafkaProducer<OwnershipEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.ownership",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = OwnershipEventDto::class.java,
            defaultTopic = UnionEventTopicProvider.getOwnershipTopic(applicationEnvironmentInfo().name),
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers()
        )
    }
}
