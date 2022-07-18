package com.rarible.protocol.union.integration.aptos

import com.rarible.protocol.aptos.api.client.AptosApiClientFactory
import com.rarible.protocol.aptos.api.client.DefaultApi
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.aptos.deserializer.AptosRoyaltiesDeserializer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@AptosConfiguration
@Import(CoreConfiguration::class)
@EnableConfigurationProperties(AptosIntegrationProperties::class)
class AptosApiConfiguration {

    @Bean
    fun aptosBlockchain(): BlockchainDto = BlockchainDto.APTOS

    @Bean
    fun aptosDefaultApi(factory: AptosApiClientFactory): DefaultApi =
        factory.createDefaultApiClient()

    @Bean
    fun aptosRoyaltiesDeserializer(): AptosRoyaltiesDeserializer = AptosRoyaltiesDeserializer()
}
