package com.rarible.protocol.union.integration.aptos

import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.integration.aptos.event.AptosItemEventHandler
import com.rarible.protocol.union.integration.aptos.event.AptosOwnershipEventHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@AptosConfiguration
@Import(AptosApiConfiguration::class)
class AptosConsumerConfiguration {

    @Bean
    fun aptosItemEventHandler(handler: IncomingEventHandler<UnionItemEvent>): AptosItemEventHandler {
        return AptosItemEventHandler(handler)
    }

    @Bean
    fun aptosOwnershipEventHandler(handler: IncomingEventHandler<UnionOwnershipEvent>): AptosOwnershipEventHandler {
        return AptosOwnershipEventHandler(handler)
    }
}
