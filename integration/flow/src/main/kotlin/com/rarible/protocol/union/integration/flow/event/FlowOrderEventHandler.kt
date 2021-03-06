package com.rarible.protocol.union.integration.flow.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOrderUpdateEventDto
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.flow.converter.FlowOrderConverter
import org.slf4j.LoggerFactory

open class FlowOrderEventHandler(
    override val handler: IncomingEventHandler<UnionOrderEvent>,
    private val flowOrderConverter: FlowOrderConverter
) : AbstractBlockchainEventHandler<FlowOrderEventDto, UnionOrderEvent>(BlockchainDto.FLOW) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureTransaction("OrderEvent#FLOW")
    override suspend fun handle(event: FlowOrderEventDto) {
        logger.info("Received {} Order event: {}", blockchain, event)

        when (event) {
            is FlowOrderUpdateEventDto -> {
                val unionOrder = flowOrderConverter.convert(event.order, blockchain)
                handler.onEvent(UnionOrderUpdateEvent(unionOrder))
            }
        }
    }
}
