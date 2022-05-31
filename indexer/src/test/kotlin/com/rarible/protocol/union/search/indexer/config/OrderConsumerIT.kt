package com.rarible.protocol.union.search.indexer.config

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OrderUpdateEventDto
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsOrderRepository
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import com.rarible.protocol.union.search.indexer.test.orderEth
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.test.context.ContextConfiguration
import java.time.Duration
import java.time.temporal.ChronoUnit

@IntegrationTest
class OrderConsumerIT {

    @Autowired
    private lateinit var producer: RaribleKafkaProducer<OrderEventDto>

    @Autowired
    private lateinit var esOrderRepository: EsOrderRepository

    @Test
    fun `should consume and save order event`(): Unit = runBlocking {
        // given
        val order = orderEth()
        val orderMsg = OrderUpdateEventDto(
            eventId = randomString(),
            orderId = order.id,
            order = order
        )

        // when
        val message = KafkaMessage<OrderEventDto>(
            key = "key",
            value = orderMsg
        )
        producer.send(message).ensureSuccess()

        // then
        Wait.waitAssert(Duration.of(2, ChronoUnit.SECONDS)) {
            val order1 = esOrderRepository.findById(order.id.toString())
            assertThat(order1?.orderId).isEqualTo(order.id.fullId())
        }
    }
}