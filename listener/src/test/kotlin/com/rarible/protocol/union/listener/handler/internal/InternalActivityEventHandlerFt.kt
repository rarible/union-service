package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.kafka.KafkaMessage
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemBurnActivity
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

@IntegrationTest
class InternalActivityEventHandlerFt : AbstractIntegrationTest() {

    @Test
    fun `internal activity event`() = runWithKafka {

        val activity = randomEthItemBurnActivity()

        ethActivityProducer.send(
            KafkaMessage(
                key = activity.id,
                value = activity
            )
        ).ensureSuccess()

        waitAssert {
            val messages = findActivityUpdates(activity.id, BurnActivityDto::class.java)
            Assertions.assertThat(messages).hasSize(1)
        }
    }
}
