package com.rarible.protocol.union.listener.handler.flow

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.union.core.flow.converter.FlowActivityConverter
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.event.KafkaEventFactory
import com.rarible.protocol.union.listener.handler.BlockchainEventHandler
import org.slf4j.LoggerFactory

class FlowActivityEventHandler(
    override val blockchain: BlockchainDto,
    private val producer: RaribleKafkaProducer<ActivityDto>,
    private val flowActivityConverter: FlowActivityConverter
) : BlockchainEventHandler<FlowActivityDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: FlowActivityDto) {
        logger.debug("Received Flow ({}) Activity event: type={}", event::class.java.simpleName)

        val unionEventDto = flowActivityConverter.convert(event, blockchain)

        producer.send(KafkaEventFactory.activityEvent(unionEventDto)).ensureSuccess()
    }
}
