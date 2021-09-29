package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OrderUpdateEventDto

object EthOrderEventConverter {

    fun convert(source: com.rarible.protocol.dto.OrderEventDto, blockchain: BlockchainDto): OrderEventDto {
        return when (source) {
            is com.rarible.protocol.dto.OrderUpdateEventDto -> {
                val order = EthOrderConverter.convert(source.order, blockchain)
                OrderUpdateEventDto(
                    eventId = source.eventId,
                    orderId = order.id,
                    order = order
                )
            }
        }
    }
}

