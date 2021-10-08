package com.rarible.protocol.union.api.controller.test

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftCollectionControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftItemControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftOrderActivityControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftOwnershipControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowOrderControllerApi
import com.rarible.protocol.nft.api.client.NftActivityControllerApi
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.nft.api.client.NftIndexerApiClientFactory
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
import com.rarible.protocol.order.api.client.OrderIndexerApiClientFactory
import com.rarible.protocol.union.api.client.FixedUnionApiServiceUriProvider
import com.rarible.protocol.union.api.client.UnionApiClientFactory
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.test.mock.CurrencyMock
import io.mockk.mockk
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import java.net.URI

@Lazy
@Configuration
class TestApiConfiguration {

    @Bean
    @Qualifier("testLocalhostUri")
    fun testLocalhostUri(@LocalServerPort port: Int): URI {
        return URI("http://localhost:${port}")
    }

    @Bean
    fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
        return ApplicationEnvironmentInfo("test", "test.com")
    }

    @Bean
    fun testRestTemplate(mapper: ObjectMapper): RestTemplate {
        val converter = MappingJackson2HttpMessageConverter()
        converter.setObjectMapper(mapper)
        val template = RestTemplate()
        template.messageConverters.add(0, converter)
        return template
    }

    @Bean
    @Primary
    fun testItemEventProducer(): RaribleKafkaProducer<ItemEventDto> = mockk()

    @Bean
    @Primary
    fun testOwnershipEventProducer(): RaribleKafkaProducer<OwnershipEventDto> = mockk()

    @Bean
    @Primary
    fun testUnionApiClientFactory(@Qualifier("testLocalhostUri") uri: URI): UnionApiClientFactory {
        return UnionApiClientFactory(FixedUnionApiServiceUriProvider(uri))
    }

    //--------------------- UNION CLIENTS ---------------------//

    @Bean
    fun testItemControllerApi(factory: UnionApiClientFactory) = factory.createItemApiClient()

    @Bean
    fun testOwnershipControllerApi(factory: UnionApiClientFactory) = factory.createOwnershipApiClient()

    @Bean
    fun testOrderControllerApi(factory: UnionApiClientFactory) = factory.createOrderApiClient()

    @Bean
    fun testSignatureControllerApi(factory: UnionApiClientFactory) = factory.createSignatureApiClient()

    @Bean
    fun testCollectionControllerApi(factory: UnionApiClientFactory) = factory.createCollectionApiClient()

    @Bean
    fun testActivityControllerApi(factory: UnionApiClientFactory) = factory.createActivityApiClient()

    @Bean
    fun testCurrencyControllerApi(factory: UnionApiClientFactory) = factory.createCurrencyApiClient()

    //--------------------- CURRENCY ---------------------//

    @Bean
    @Primary
    fun testCurrencyApi(): CurrencyControllerApi = CurrencyMock.currencyControllerApiMock

    //--------------------- ETHEREUM ---------------------//
    @Bean
    @Primary
    @Qualifier("ethereum.item.api")
    fun testEthereumItemApi(): NftItemControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("ethereum.ownership.api")
    fun testEthereumOwnershipApi(): NftOwnershipControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("ethereum.collection.api")
    fun testEthereumCollectionApi(): NftCollectionControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("ethereum.order.api")
    fun testEthereumOrderApi(): com.rarible.protocol.order.api.client.OrderControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("ethereum.signature.api")
    fun testEthereumSignatureApi(): com.rarible.protocol.order.api.client.OrderSignatureControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("ethereum.activity.api.item")
    fun testEthereumActivityItemApi(factory: NftIndexerApiClientFactory): NftActivityControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("ethereum.activity.api.order")
    fun testEthereumActivityOrderApi(factory: OrderIndexerApiClientFactory): OrderActivityControllerApi = mockk()

    //--------------------- POLYGON ---------------------//
    @Bean
    @Primary
    @Qualifier("polygon.item.api")
    fun testPolygonNftItemApi(): NftItemControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("polygon.ownership.api")
    fun testPolygonNftOwnershipApi(): NftOwnershipControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("polygon.collection.api")
    fun testPolygonNftCollectionApi(): NftCollectionControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("polygon.order.api")
    fun testPolygonOrderApi(): com.rarible.protocol.order.api.client.OrderControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("polygon.signature.api")
    fun testPolygonSignatureApi(): com.rarible.protocol.order.api.client.OrderSignatureControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("polygon.activity.api.item")
    fun testPolygonActivityItemApi(factory: NftIndexerApiClientFactory): NftActivityControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("polygon.activity.api.order")
    fun testPolygonActivityOrderApi(factory: OrderIndexerApiClientFactory): OrderActivityControllerApi = mockk()

    //--------------------- FLOW ---------------------//
    @Bean
    @Primary
    fun testFlowItemApi(): FlowNftItemControllerApi = mockk()

    @Bean
    @Primary
    fun testFlowOwnershipApi(): FlowNftOwnershipControllerApi = mockk()

    @Bean
    @Primary
    fun testFlowCollectionApi(): FlowNftCollectionControllerApi = mockk()

    @Bean
    @Primary
    fun testFlowOrderApi(): FlowOrderControllerApi = mockk()

    @Bean
    @Primary
    fun testFlowActivityApi(): FlowNftOrderActivityControllerApi = mockk()

}
