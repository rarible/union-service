package com.rarible.protocol.union.api.configuration

import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.rarible.core.autoconfigure.filter.cors.EnableRaribleCorsWebFilter
import com.rarible.protocol.union.dto.UnionModelJacksonModule
import com.rarible.protocol.union.dto.UnionPrimitivesJacksonModule
import com.rarible.protocol.union.enrichment.configuration.EnrichmentApiConfiguration
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
@EnableRaribleCorsWebFilter
@Import(EnrichmentApiConfiguration::class)
@EnableConfigurationProperties(value = [OpenapiProperties::class])
class ApiConfiguration {

    @Bean
    fun jsonCustomizer(deserializers: List<JsonDeserializer<Any>>): Jackson2ObjectMapperBuilderCustomizer? {
        return Jackson2ObjectMapperBuilderCustomizer { builder: Jackson2ObjectMapperBuilder ->
            builder.modules(
                UnionPrimitivesJacksonModule,
                UnionModelJacksonModule,
                KotlinModule(),
                JavaTimeModule()
            )
            builder.deserializers(*deserializers.toTypedArray())
        }
    }

    @Bean
    fun corsFilter(corsConfigurationSource: CorsConfigurationSource): CorsWebFilter {
        return CorsWebFilter(corsConfigurationSource)
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration().applyPermitDefaultValues()
        config.addAllowedMethod(HttpMethod.GET)
        config.addAllowedMethod(HttpMethod.POST)
        config.addAllowedMethod(HttpMethod.DELETE)
        config.maxAge = 3600
        source.registerCorsConfiguration("/**", config)
        return source
    }
}
