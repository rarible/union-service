package com.rarible.protocol.union.integration.aptos.event

import com.rarible.protocol.dto.aptos.AptosCollectionEventDto
import com.rarible.protocol.dto.aptos.AptosCollectionUpdateEventDto
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.aptos.converter.AptosCollectionConverter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AptosCollectionEventHandler(override val handler: IncomingEventHandler<UnionCollectionEvent>) :
    AbstractBlockchainEventHandler<AptosCollectionEventDto, UnionCollectionEvent>(BlockchainDto.APTOS) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: AptosCollectionEventDto) {
        logger.info("Received {} Collection event: {}", blockchain, event)

        when (event) {
            is AptosCollectionUpdateEventDto -> {
                val collection = AptosCollectionConverter.convert(event.collection, blockchain)
                handler.onEvent(UnionCollectionUpdateEvent(collection))
            }
        }
    }
}
