package com.rarible.protocol.union.enrichment.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("meta")
data class MetaProperties(
    val ipfsGateway: String,
    val mediaFetchTimeout: Int,
    val openSeaProxyUrl: String
)