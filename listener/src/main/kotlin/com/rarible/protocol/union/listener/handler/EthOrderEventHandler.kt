package com.rarible.protocol.nftorder.listener.handler

import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EthOrderEventHandler : AbstractEventHandler<OrderEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: OrderEventDto) {
        logger.debug("Received Ethereum Order event: type=${event::class.java.simpleName}")
        // TODO - Implement
    }

}