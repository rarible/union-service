package com.rarible.protocol.union.search.indexer.config

import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.integration.ethereum.EthereumApiConfiguration
import com.rarible.protocol.union.integration.ethereum.PolygonApiConfiguration
import com.rarible.protocol.union.integration.flow.FlowApiConfiguration
import com.rarible.protocol.union.integration.immutablex.ImmutablexApiConfiguration
import com.rarible.protocol.union.integration.solana.SolanaApiConfiguration
import org.elasticsearch.action.support.WriteRequest
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.util.function.Supplier

@Configuration
@EnableConfigurationProperties(value = [KafkaProperties::class, IndexerProperties::class])
@ComponentScan(basePackageClasses = [EsActivityRepository::class])
@Import(
    value = [
        EthereumApiConfiguration::class,
        PolygonApiConfiguration::class,
        FlowApiConfiguration::class,
        // TezosApiConfiguration::class, // TODO enable with proper config
        SolanaApiConfiguration::class,
        ImmutablexApiConfiguration::class,
        SearchConfiguration::class,
        CoreConfiguration::class,
    ]
)
class UnionIndexerConfiguration