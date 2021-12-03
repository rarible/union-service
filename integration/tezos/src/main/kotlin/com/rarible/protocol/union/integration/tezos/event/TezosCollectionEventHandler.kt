package com.rarible.protocol.union.integration.tezos.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.tezos.dto.TezosCollectionSafeEventDto
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.CollectionUpdateEventDto
import com.rarible.protocol.union.integration.tezos.converter.TezosCollectionConverter
import org.slf4j.LoggerFactory

open class TezosCollectionEventHandler(
    override val handler: IncomingEventHandler<CollectionEventDto>
) : AbstractBlockchainEventHandler<TezosCollectionSafeEventDto, CollectionEventDto>(BlockchainDto.TEZOS) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureTransaction("ItemEvent#TEZOS")
    override suspend fun handle(event: TezosCollectionSafeEventDto) {
        logger.info("Received Tezos Item event: type={}", event::class.java.simpleName)

        when (event.type) {
            TezosCollectionSafeEventDto.Type.UPDATE -> {
                val collection = TezosCollectionConverter.convert(event.collection!!, blockchain)
                val unionCollectionEvent = CollectionUpdateEventDto(
                    collectionId = collection.id,
                    eventId = event.eventId!!,
                    collection = collection
                )
                handler.onEvent(unionCollectionEvent)
            }

            TezosCollectionSafeEventDto.Type.SERIALIZATION_FAILED -> {
                // skip it, will be logged inside of parser
            }
        }
    }
}