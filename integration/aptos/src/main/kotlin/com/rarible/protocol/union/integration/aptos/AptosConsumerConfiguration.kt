package com.rarible.protocol.union.integration.aptos

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.aptos.api.subscriber.AptosEventsConsumerFactory
import com.rarible.protocol.dto.aptos.AptosCollectionEventDto
import com.rarible.protocol.dto.aptos.AptosOwnershipEventDto
import com.rarible.protocol.dto.aptos.AptosTokenEventDto
import com.rarible.protocol.union.core.ConsumerFactory
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.handler.KafkaConsumerWorker
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.integration.aptos.event.AptosCollectionEventHandler
import com.rarible.protocol.union.integration.aptos.event.AptosItemEventHandler
import com.rarible.protocol.union.integration.aptos.event.AptosOwnershipEventHandler
import com.rarible.protocol.union.integration.aptos.service.AptosUpdateService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@AptosConfiguration
@Import(AptosApiConfiguration::class)
class AptosConsumerConfiguration(
    private val consumerFactory: ConsumerFactory,
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    properties: AptosIntegrationProperties,
) {

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host

    private val consumer = properties.consumer!!
    private val workers = properties.consumer!!.workers

    private val daemon = properties.daemon

    @Bean
    fun aptosItemEventHandler(handler: IncomingEventHandler<UnionItemEvent>): AptosItemEventHandler {
        return AptosItemEventHandler(handler)
    }

    @Bean
    fun aptosOwnershipEventHandler(handler: IncomingEventHandler<UnionOwnershipEvent>): AptosOwnershipEventHandler {
        return AptosOwnershipEventHandler(handler)
    }

    @Bean
    fun aptosCollectionEventHandler(handler: IncomingEventHandler<UnionCollectionEvent>): AptosCollectionEventHandler {
        return AptosCollectionEventHandler(handler)
    }

    @Bean
    fun aptosConsumerFactory(): AptosEventsConsumerFactory {
        return AptosEventsConsumerFactory(
            brokerReplicaSet = consumer.brokerReplicaSet!!,
            environment = env,
            host = host
        )
    }

    @Bean
    fun aptosItemWorker(
        factory: AptosEventsConsumerFactory,
        handler: BlockchainEventHandler<AptosTokenEventDto, UnionItemEvent>
    ): KafkaConsumerWorker<AptosTokenEventDto> {
        val consumer = factory.createItemEventsConsumer(consumerFactory.itemGroup)
        return consumerFactory.createItemConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun aptosOwnershipWorker(
        factory: AptosEventsConsumerFactory,
        handler: BlockchainEventHandler<AptosOwnershipEventDto, UnionOwnershipEvent>
    ): KafkaConsumerWorker<AptosOwnershipEventDto> {
        val consumer = factory.createOwnershipEventsConsumer(consumerFactory.ownershipGroup)
        return consumerFactory.createOwnershipConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun aptosCollectionWorker(
        factory: AptosEventsConsumerFactory,
        handler: BlockchainEventHandler<AptosCollectionEventDto, UnionCollectionEvent>
    ): KafkaConsumerWorker<AptosCollectionEventDto> {
        val consumer = factory.createCollectionEventsConsumer(consumerFactory.collectionGroup)
        return consumerFactory.createCollectionConsumer(consumer, handler, daemon, workers)
    }

    @Bean
    fun aptosUpdateService(
        aptosItemEventHandler: AptosItemEventHandler,
        aptosOwnershipEventHandler: AptosOwnershipEventHandler,
        aptosCollectionEventHandler: AptosCollectionEventHandler
    ): AptosUpdateService = AptosUpdateService(
        itemEventHandler = aptosItemEventHandler,
        ownershipEventHandler = aptosOwnershipEventHandler,
        collectionEventHandler = aptosCollectionEventHandler
    )
}
