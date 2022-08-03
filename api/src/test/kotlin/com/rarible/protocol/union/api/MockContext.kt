package com.rarible.protocol.union.api

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.logging.Logger
import com.rarible.core.test.ext.KafkaTestExtension
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemEventTopicProvider
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipEventTopicProvider
import com.rarible.protocol.union.api.configuration.WebSocketConfiguration
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.elasticsearch.EsRepository
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig
import com.rarible.protocol.union.dto.FakeSubscriptionEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.SubscriptionEventDto
import com.rarible.protocol.union.dto.SubscriptionRequestDto
import com.rarible.protocol.union.dto.UnionEventTopicProvider
import com.rarible.protocol.union.subscriber.UnionKafkaJsonDeserializer
import com.rarible.protocol.union.subscriber.UnionKafkaJsonSerializer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.web.context.WebServerInitializedEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Sinks
import java.net.URI
import java.util.concurrent.LinkedBlockingQueue

@Configuration
@EnableAutoConfiguration
@Import(WebSocketConfiguration::class)
class MockContext : ApplicationListener<WebServerInitializedEvent> {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var applicationEnvironmentInfo: ApplicationEnvironmentInfo

    @Bean
    fun shutdownMono(): Sinks.One<Void> = Sinks.one()

    @Bean
    fun webSocketEventsQueue() = LinkedBlockingQueue<SubscriptionEventDto>()

    @Bean
    fun webSocketRequests(): Sinks.Many<List<SubscriptionRequestDto>> = Sinks.many().unicast().onBackpressureBuffer()

    @Bean(initMethod = "bootstrap")
    @ConditionalOnMissingBean
    fun elasticsearchBootstrap(
        reactiveElasticSearchOperations: ReactiveElasticsearchOperations,
        restHighLevelClient: ReactiveElasticsearchClient,
        esNameResolver: EsNameResolver,
        indexService: IndexService,
        repositories: List<EsRepository>,
    ): ElasticsearchTestBootstrapper {

        return ElasticsearchTestBootstrapper(
            esNameResolver = esNameResolver,
            esOperations = reactiveElasticSearchOperations,
            restHighLevelClient,
            entityDefinitions = EsEntitiesConfig.createEsEntities(),
            repositories = repositories
        )
    }

    /* @Bean
     fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
         return ApplicationEnvironmentInfo("test", "test.com")
     }*/

    @Bean
    fun testItemConsumer(): RaribleKafkaConsumer<ItemEventDto> {
        val topic = UnionEventTopicProvider.getItemTopic(applicationEnvironmentInfo.name)
        return RaribleKafkaConsumer(
            clientId = "test-union-item-consumer",
            consumerGroup = "test-union-item-group",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = ItemEventDto::class.java,
            defaultTopic = topic,
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers(),
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    @Bean
    fun testOwnershipConsumer(): RaribleKafkaConsumer<OwnershipEventDto> {
        val topic = UnionEventTopicProvider.getOwnershipTopic(applicationEnvironmentInfo.name)
        return RaribleKafkaConsumer(
            clientId = "test-union-ownership-consumer",
            consumerGroup = "test-union-ownership-group",
            valueDeserializerClass = UnionKafkaJsonDeserializer::class.java,
            valueClass = OwnershipEventDto::class.java,
            defaultTopic = topic,
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers(),
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
    }

    @Bean
    fun testEthereumItemEventProducer(): RaribleKafkaProducer<NftItemEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.ethereum.item",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = NftItemEventDto::class.java,
            defaultTopic = NftItemEventTopicProvider.getTopic(applicationEnvironmentInfo.name, "ethereum"),
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    fun testEthereumOwnershipEventProducer(): RaribleKafkaProducer<NftOwnershipEventDto> {
        return RaribleKafkaProducer(
            clientId = "test.union.ethereum.ownership",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = NftOwnershipEventDto::class.java,
            defaultTopic = NftOwnershipEventTopicProvider.getTopic(applicationEnvironmentInfo.name, "ethereum"),
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers()
        )
    }

    @Bean
    @Qualifier("item.producer.api")
    fun unionItemEventProducer(): RaribleKafkaProducer<ItemEventDto> {
        val itemTopic = UnionEventTopicProvider.getItemTopic("test")
        return createUnionProducer("item", itemTopic, ItemEventDto::class.java)
    }

    private fun <T> createUnionProducer(clientSuffix: String, topic: String, type: Class<T>): RaribleKafkaProducer<T> {
        return RaribleKafkaProducer(
            clientId = "test.protocol-union-service.${clientSuffix}",
            valueSerializerClass = UnionKafkaJsonSerializer::class.java,
            valueClass = type,
            defaultTopic = topic,
            bootstrapServers = KafkaTestExtension.kafkaContainer.kafkaBoostrapServers()
        )
    }

    override fun onApplicationEvent(event: WebServerInitializedEvent) {
        val port = event.webServer.port

        ReactorNettyWebSocketClient().execute(URI("ws://127.0.0.1:$port/v0.1/subscribe")) { session ->
            logger.info("Connected to ws")
            session.receive().map { notification ->
                logger.info("received from ws: {}", notification.payloadAsText)
                val evt: SubscriptionEventDto = objectMapper.readValue(notification.payloadAsText)
                if (evt !is FakeSubscriptionEventDto) {
                    webSocketEventsQueue().offer(evt)
                }
            }.subscribe()

            session.send(webSocketRequests().asFlux().map {
                val bytes = objectMapper.writerFor(typeRef).writeValueAsBytes(it)
                logger.info("sending to ws: {}", String(bytes))
                WebSocketMessage(
                    WebSocketMessage.Type.TEXT, DefaultDataBufferFactory().wrap(bytes)
                )
            }).doOnSubscribe { logger.info("subscribed to ws") }
                .subscribe({}, { logger.error("ws error", it) }, { logger.info("disconnected from ws") })
            shutdownMono().asMono()
        }.subscribe()
    }

    companion object {
        private val logger by Logger()
    }
}

val typeRef = object : TypeReference<List<SubscriptionRequestDto>>() {}
