package com.rarible.protocol.union.enrichment.configuration

import com.rarible.protocol.union.integration.aptos.AptosApiConfiguration
import com.rarible.protocol.union.integration.ethereum.EthereumApiConfiguration
import com.rarible.protocol.union.integration.ethereum.PolygonApiConfiguration
import com.rarible.protocol.union.integration.flow.FlowApiConfiguration
import com.rarible.protocol.union.integration.immutablex.ImmutablexApiConfiguration
import com.rarible.protocol.union.integration.solana.SolanaApiConfiguration
import com.rarible.protocol.union.integration.tezos.TezosApiConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(
    value = [
        EnrichmentConfiguration::class,
        UnionMetaConfiguration::class,
        EthereumApiConfiguration::class,
        PolygonApiConfiguration::class,
        FlowApiConfiguration::class,
        TezosApiConfiguration::class,
        SolanaApiConfiguration::class,
        ImmutablexApiConfiguration::class,
        AptosApiConfiguration::class
    ]
)
class EnrichmentApiConfiguration
