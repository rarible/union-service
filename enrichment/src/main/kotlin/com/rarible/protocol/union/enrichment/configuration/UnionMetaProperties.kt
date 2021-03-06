package com.rarible.protocol.union.enrichment.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("meta")
data class UnionMetaProperties(
    val ipfsGateway: String,
    val ipfsPublicGateway: String,
    val ipfsLegacyGateway: String?,
    val mediaFetchMaxSize: Long,
    val openSeaProxyUrl: String,
    val embedded: EmbeddedContentProperties,
    val httpClient: HttpClient = HttpClient()
){
    class HttpClient(
        val type: HttpClientType = HttpClientType.KTOR_CIO,
        val threadCount: Int = 8,
        val timeOut: Int = 5000,
        val totalConnection: Int = 500,
        val connectionsPerRoute: Int = 20,
        val keepAlive: Boolean = true
    ) {

        enum class HttpClientType {
            KTOR_APACHE,
            KTOR_CIO,
            ASYNC_APACHE
        }
    }
}

data class EmbeddedContentProperties(
    val publicUrl: String,
    @Deprecated("Should be removed after migration")
    val legacyUrls: String = ""
)
