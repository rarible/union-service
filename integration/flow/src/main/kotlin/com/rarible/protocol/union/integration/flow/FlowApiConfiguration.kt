package com.rarible.protocol.union.integration.flow

import com.rarible.protocol.flow.nft.api.client.FlowNftCollectionControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftCryptoControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftIndexerApiClientFactory
import com.rarible.protocol.flow.nft.api.client.FlowNftItemControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftOrderActivityControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftOwnershipControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowOrderControllerApi
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.OrderProxyService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.integration.flow.converter.FlowActivityConverter
import com.rarible.protocol.union.integration.flow.converter.FlowOrderConverter
import com.rarible.protocol.union.integration.flow.service.FlowActivityService
import com.rarible.protocol.union.integration.flow.service.FlowCollectionService
import com.rarible.protocol.union.integration.flow.service.FlowItemService
import com.rarible.protocol.union.integration.flow.service.FlowOrderService
import com.rarible.protocol.union.integration.flow.service.FlowOwnershipService
import com.rarible.protocol.union.integration.flow.service.FlowSignatureService
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import

@FlowConfiguration
@Import(CoreConfiguration::class)
@ComponentScan(basePackageClasses = [FlowOrderConverter::class])
@EnableConfigurationProperties(value = [FlowIntegrationProperties::class])
class FlowApiConfiguration(
    private val properties: FlowIntegrationProperties
) {

    private val flow = BlockchainDto.FLOW.name.toLowerCase()

    @Bean
    fun flowBlockchain(): BlockchainDto {
        return BlockchainDto.FLOW
    }

    //-------------------- API --------------------//

    @Bean
    fun flowItemApi(factory: FlowNftIndexerApiClientFactory): FlowNftItemControllerApi =
        factory.createNftItemApiClient(flow)

    @Bean
    fun flowOwnershipApi(factory: FlowNftIndexerApiClientFactory): FlowNftOwnershipControllerApi =
        factory.createNftOwnershipApiClient(flow)

    @Bean
    fun flowCollectionApi(factory: FlowNftIndexerApiClientFactory): FlowNftCollectionControllerApi =
        factory.createNftCollectionApiClient(flow)

    @Bean
    fun flowOrderApi(factory: FlowNftIndexerApiClientFactory): FlowOrderControllerApi =
        factory.createNftOrderApiClient(flow)

    @Bean
    fun flowActivityApi(factory: FlowNftIndexerApiClientFactory): FlowNftOrderActivityControllerApi =
        factory.createNftOrderActivityApiClient(flow)

    @Bean
    fun flowCryptoApi(factory: FlowNftIndexerApiClientFactory): FlowNftCryptoControllerApi =
        factory.createCryptoApiClient(flow)

    //-------------------- Services --------------------//

    @Bean
    fun flowItemService(controllerApi: FlowNftItemControllerApi): FlowItemService {
        return FlowItemService(controllerApi)
    }

    @Bean
    fun flowOwnershipService(controllerApi: FlowNftOwnershipControllerApi): FlowOwnershipService {
        return FlowOwnershipService(controllerApi)
    }

    @Bean
    fun flowCollectionService(controllerApi: FlowNftCollectionControllerApi): FlowCollectionService {
        return FlowCollectionService(controllerApi)
    }

    @Bean
    fun flowOrderService(
        controllerApi: FlowOrderControllerApi,
        converter: FlowOrderConverter
    ): OrderService {
        return OrderProxyService(
            FlowOrderService(controllerApi, converter),
            setOf(PlatformDto.RARIBLE)
        )
    }

    @Bean
    fun flowSignatureService(controllerApi: FlowNftCryptoControllerApi): FlowSignatureService {
        return FlowSignatureService(controllerApi)
    }

    @Bean
    fun flowActivityService(
        activityApi: FlowNftOrderActivityControllerApi,
        converter: FlowActivityConverter
    ): FlowActivityService {
        return FlowActivityService(activityApi, converter)
    }
}