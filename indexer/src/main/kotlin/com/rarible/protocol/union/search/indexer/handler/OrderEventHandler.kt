package com.rarible.protocol.union.search.indexer.handler

import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.converter.EsOrderConverter
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.enrichment.repository.search.EsOrderRepository
import org.springframework.stereotype.Service

@Service
class OrderEventHandler(
    private val featureFlagsProperties: FeatureFlagsProperties,
    private val repository: EsOrderRepository
): ConsumerBatchEventHandler<OrderEventDto> {

    private val logger by Logger()

    override suspend fun handle(event: List<OrderEventDto>) {
        logger.info("Handling ${event.size} OrderDto events")

        val convertedEvents = event.map {
            logger.info("Converting OrderDto id = ${it.orderId}")
            EsOrderConverter.convert(it)
        }
        logger.info("Saving ${convertedEvents.size} OrderDto events to ElasticSearch")
        val refreshPolicy = featureFlagsProperties.orderRefreshPolicy
        val saved = repository.saveAll(convertedEvents, refreshPolicy = refreshPolicy)
        logger.info("Handling completed, {}", saved.map { it.orderId })
    }
}
