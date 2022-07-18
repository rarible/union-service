package com.rarible.protocol.union.integration.aptos.event

import com.rarible.protocol.dto.aptos.AptosTokenDeleteEventDto
import com.rarible.protocol.dto.aptos.AptosTokenEventDto
import com.rarible.protocol.dto.aptos.AptosTokenUpdateEventDto
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.integration.aptos.converter.AptosItemConverter
import org.slf4j.LoggerFactory

class AptosItemEventHandler(override val handler: IncomingEventHandler<UnionItemEvent>) :
    AbstractBlockchainEventHandler<AptosTokenEventDto, UnionItemEvent>(BlockchainDto.APTOS) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: AptosTokenEventDto) {
        logger.info("Received {} Item event: {}", blockchain, event)

        when(event) {
            is AptosTokenUpdateEventDto -> {
                val item = AptosItemConverter.convert(event.item, blockchain)
                handler.onEvent(UnionItemUpdateEvent(item))
            }
            is AptosTokenDeleteEventDto -> {
                val itemId = ItemIdDto(
                    blockchain = blockchain,
                    value = event.itemId
                )
                handler.onEvent(UnionItemDeleteEvent(itemId))
            }
        }
    }


}
