package com.rarible.protocol.union.listener.config

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.task.EnableRaribleTask
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import com.rarible.protocol.dto.*
import com.rarible.protocol.flow.nft.api.subscriber.FlowNftIndexerEventsConsumerFactory
import com.rarible.protocol.nft.api.subscriber.NftIndexerEventsConsumerFactory
import com.rarible.protocol.order.api.subscriber.OrderIndexerEventsConsumerFactory
import com.rarible.protocol.union.dto.UnionEventTopicProvider
import com.rarible.protocol.union.dto.UnionItemEventDto
import com.rarible.protocol.union.dto.UnionOrderEventDto
import com.rarible.protocol.union.dto.UnionOwnershipEventDto
import com.rarible.protocol.union.listener.handler.ethereum.EthereumCompositeConsumerWorker
import com.rarible.protocol.union.listener.handler.ethereum.EthereumCompositeConsumerWorker.ConsumerEventHandlerFactory
import com.rarible.protocol.union.listener.handler.ethereum.EthereumCompositeConsumerWorker.ConsumerFactory
import com.rarible.protocol.union.listener.handler.ethereum.EthereumEventHandlerFactory
import com.rarible.protocol.union.listener.handler.flow.FlowItemEventHandler
import com.rarible.protocol.union.listener.handler.flow.FlowOwnershipEventHandler
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableRaribleTask
@EnableScaletherMongoConversions
@EnableConfigurationProperties(
    value = [
        UnionEventProducerProperties::class,
        UnionListenerProperties::class
    ]
)
class UnionListenerConfiguration(
    environmentInfo: ApplicationEnvironmentInfo,
    private val listenerProperties: UnionListenerProperties,
    private val producerProperties: UnionEventProducerProperties,
    private val meterRegistry: MeterRegistry
) {
    private val itemConsumerGroup = "${environmentInfo.name}.protocol.union.item"
    private val ownershipConsumerGroup = "${environmentInfo.name}.protocol.union.ownership"
    private val orderConsumerGroup = "${environmentInfo.name}.protocol.union.order"

    @Bean
    fun ethereumItemChangeWorker(
        nftIndexerEventsConsumerFactory: NftIndexerEventsConsumerFactory,
        eventHandlerFactory: EthereumEventHandlerFactory
    ): EthereumCompositeConsumerWorker<NftItemEventDto> {
        return EthereumCompositeConsumerWorker(
            consumerFactory = ConsumerFactory.wrap { group, blockchain ->
                nftIndexerEventsConsumerFactory.createItemEventsConsumer(group, blockchain)
            },
            eventHandlerFactory = ConsumerEventHandlerFactory.wrap { blockchain ->
                eventHandlerFactory.createItemEventHandler(blockchain)
            },
            consumerGroup = itemConsumerGroup,
            properties = listenerProperties.monitoringWorker,
            meterRegistry = meterRegistry,
            workerName = "itemEventDto"
        )
    }

    @Bean
    fun ethereumOwnershipChangeWorker(
        nftIndexerEventsConsumerFactory: NftIndexerEventsConsumerFactory,
        eventHandlerFactory: EthereumEventHandlerFactory
    ): EthereumCompositeConsumerWorker<NftOwnershipEventDto> {
        return EthereumCompositeConsumerWorker(
            consumerFactory = ConsumerFactory.wrap { group, blockchain ->
                nftIndexerEventsConsumerFactory.createOwnershipEventsConsumer(group, blockchain)
            },
            eventHandlerFactory = ConsumerEventHandlerFactory.wrap { blockchain ->
                eventHandlerFactory.createOwnershipEventHandler(blockchain)
            },
            consumerGroup = ownershipConsumerGroup,
            properties = listenerProperties.monitoringWorker,
            meterRegistry = meterRegistry,
            workerName = "ownershipEventDto"
        )
    }

    @Bean
    fun ethereumOrderChangeWorker(
        orderIndexerEventsConsumerFactory: OrderIndexerEventsConsumerFactory,
        eventHandlerFactory: EthereumEventHandlerFactory
    ): EthereumCompositeConsumerWorker<OrderEventDto> {
        return EthereumCompositeConsumerWorker(
            consumerFactory = ConsumerFactory.wrap { group, blockchain ->
                orderIndexerEventsConsumerFactory.createOrderEventsConsumer(group, blockchain)
            },
            eventHandlerFactory = ConsumerEventHandlerFactory.wrap { blockchain ->
                eventHandlerFactory.createOrderEventHandler(blockchain)
            },
            consumerGroup = orderConsumerGroup,
            properties = listenerProperties.monitoringWorker,
            meterRegistry = meterRegistry,
            workerName = "orderEventDto"
        )
    }

    @Bean
    fun flowItemChangeWorker(
        flowNftIndexerEventsConsumerFactory: FlowNftIndexerEventsConsumerFactory,
        flowItemEventHandler: FlowItemEventHandler
    ): ConsumerWorker<FlowNftItemEventDto> {
        return ConsumerWorker(
            consumer = flowNftIndexerEventsConsumerFactory.createItemEventsConsumer(itemConsumerGroup),
            properties = listenerProperties.monitoringWorker,
            eventHandler = flowItemEventHandler,
            meterRegistry = meterRegistry,
            workerName = "flowItemEventDto"
        )
    }

    @Bean
    fun flowOwnershipChangeWorker(
        flowNftIndexerEventsConsumerFactory: FlowNftIndexerEventsConsumerFactory,
        flowOwnershipEventHandler: FlowOwnershipEventHandler
    ): ConsumerWorker<FlowOwnershipEventDto> {
        return ConsumerWorker(
            consumer = flowNftIndexerEventsConsumerFactory.createOwnershipEventsConsumer(itemConsumerGroup),
            properties = listenerProperties.monitoringWorker,
            eventHandler = flowOwnershipEventHandler,
            meterRegistry = meterRegistry,
            workerName = "flowItemEventDto"
        )
    }

//TODO: Not correct types
//    @Bean
//    fun flowOrderChangeWorker(
//        flowNftIndexerEventsConsumerFactory: FlowNftIndexerEventsConsumerFactory,
//        flowOrderEventHandler: FlowOrderEventHandler
//    ): ConsumerWorker<FlowOrderEventDto> {
//        return ConsumerWorker(
//            consumer = flowNftIndexerEventsConsumerFactory.createORderEventsConsumer(itemConsumerGroup),
//            properties = listenerProperties.monitoringWorker,
//            eventHandler = flowOrderEventHandler,
//            meterRegistry = meterRegistry,
//            workerName = "flowOrderEventDto"
//        )
//    }

    @Bean
    fun unionItemEventProducer(): RaribleKafkaProducer<UnionItemEventDto> {
        return RaribleKafkaProducer(
            clientId = "${producerProperties.environment}.protocol-union-listener.item",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = UnionItemEventDto::class.java,
            defaultTopic = UnionEventTopicProvider.getItemTopic(producerProperties.environment),
            bootstrapServers = producerProperties.kafkaReplicaSet
        )
    }

    @Bean
    fun unionOwnershipEventProducer(): RaribleKafkaProducer<UnionOwnershipEventDto> {
        return RaribleKafkaProducer(
            clientId = "${producerProperties.environment}.protocol-union-listener.ownership",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = UnionOwnershipEventDto::class.java,
            defaultTopic = UnionEventTopicProvider.getOwnershipTopic(producerProperties.environment),
            bootstrapServers = producerProperties.kafkaReplicaSet
        )
    }

    @Bean
    fun unionOrderEventProducer(): RaribleKafkaProducer<UnionOrderEventDto> {
        return RaribleKafkaProducer(
            clientId = "${producerProperties.environment}.protocol-union-listener.order",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = UnionOrderEventDto::class.java,
            defaultTopic = UnionEventTopicProvider.getOrderTopic(producerProperties.environment),
            bootstrapServers = producerProperties.kafkaReplicaSet
        )
    }

}
