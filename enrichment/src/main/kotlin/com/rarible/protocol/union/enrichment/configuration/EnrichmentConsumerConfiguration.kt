package com.rarible.protocol.union.enrichment.configuration

import com.rarible.protocol.union.integration.aptos.AptosConsumerConfiguration
import com.rarible.protocol.union.integration.ethereum.EthereumConsumerConfiguration
import com.rarible.protocol.union.integration.ethereum.PolygonConsumerConfiguration
import com.rarible.protocol.union.integration.flow.FlowConsumerConfiguration
import com.rarible.protocol.union.integration.immutablex.ImmutablexConsumerConfiguration
import com.rarible.protocol.union.integration.solana.SolanaConsumerConfiguration
import com.rarible.protocol.union.integration.tezos.TezosConsumerConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(
    value = [
        EnrichmentApiConfiguration::class,
        EthereumConsumerConfiguration::class,
        PolygonConsumerConfiguration::class,
        FlowConsumerConfiguration::class,
        TezosConsumerConfiguration::class,
        ImmutablexConsumerConfiguration::class,
        SolanaConsumerConfiguration::class,
        AptosConsumerConfiguration::class
    ]
)
class EnrichmentConsumerConfiguration
