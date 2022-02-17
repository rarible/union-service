package com.rarible.protocol.union.listener.service

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.union.enrichment.converter.EnrichedItemConverter
import com.rarible.protocol.union.enrichment.meta.UnionMetaLoader
import com.rarible.protocol.union.enrichment.service.EnrichmentMetaService
import com.rarible.protocol.union.integration.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.coEvery
import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono
import java.time.Duration

@IntegrationTest
class EnrichmentItemMetaLoadingIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var itemEventService: EnrichmentItemEventService

    @Autowired
    private lateinit var itemMetaService: EnrichmentMetaService

    @Test
    fun `item update - meta not available - wait synchronously - send two events with meta`() = runWithKafka {
        val itemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(itemId)
        val (unionItem, meta) = EthItemConverter.convert(ethItem, itemId.blockchain).let {
            it.copy(meta = null) to it.meta!!
        }
        coEvery { testUnionMetaLoader.load(itemId) } coAnswers {
            delay(100L)
            meta
        }
        metaProperties.timeoutSyncLoadingMetaMs = 3000
        coEvery { testEthereumItemApi.getNftItemById(itemId.value) } returns ethItem.toMono()
        itemEventService.onItemUpdated(unionItem)
        Wait.waitAssert {
            val events = findItemUpdates(itemId.value)
            // We receive two events here:
            // 1) from the synchronous event handler
            // 2) from the meta loader listener
            assertThat(events).hasSize(2).allSatisfy {
                assertThat(it.value.item).isEqualTo(EnrichedItemConverter.convert(unionItem, meta = meta))
            }
        }
    }

    @Test
    fun `item update - meta not available - timeout waiting - event without meta - then with meta`() = runWithKafka {
        val itemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(itemId)
        val (unionItem, meta) = EthItemConverter.convert(ethItem, itemId.blockchain).let {
            it.copy(meta = null) to it.meta!!
        }
        coEvery { testUnionMetaLoader.load(itemId) } coAnswers {
            delay(2000L) // Long loading meta
            meta
        }
        metaProperties.timeoutSyncLoadingMetaMs = 1000
        coEvery { testEthereumItemApi.getNftItemById(itemId.value) } returns ethItem.toMono()
        itemEventService.onItemUpdated(unionItem)
        Wait.waitAssert {
            val events = findItemUpdates(itemId.value)
            assertThat(events).hasSize(1)
            assertThat(events[0].value.item)
                .isEqualTo(EnrichedItemConverter.convert(unionItem, meta = null))
        }
        Wait.waitAssert {
            val events = findItemUpdates(itemId.value)
            assertThat(events).hasSize(2)
            assertThat(events[1].value.item)
                .isEqualTo(EnrichedItemConverter.convert(unionItem, meta = meta))
        }
    }

    @Test
    fun `item update - meta not available - error loading - send 1 event without meta - then refresh`() = runWithKafka {
        val itemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(itemId)
        val (unionItem, meta) = EthItemConverter.convert(ethItem, itemId.blockchain).let {
            it.copy(meta = null) to it.meta!!
        }
        coEvery { testUnionMetaLoader.load(itemId) } coAnswers {
            delay(100L)
            throw UnionMetaLoader.UnionMetaResolutionException("error")
        }
        metaProperties.timeoutSyncLoadingMetaMs = 2000
        coEvery { testEthereumItemApi.getNftItemById(itemId.value) } returns ethItem.toMono()
        itemEventService.onItemUpdated(unionItem)
        Wait.waitAssert {
            val events = findItemUpdates(itemId.value)
            assertThat(events).hasSize(1)
            assertThat(events[0].value.item)
                .isEqualTo(EnrichedItemConverter.convert(unionItem, meta = null))
        }
        // Schedule a successful update.
        coEvery { testUnionMetaLoader.load(itemId) } coAnswers {
            delay(100L)
            meta
        }
        itemMetaService.scheduleLoading(itemId)
        // On loading meta, send an event.
        Wait.waitAssert {
            val events = findItemUpdates(itemId.value)
            assertThat(events).hasSize(2)
            assertThat(events[1].value.item)
                .isEqualTo(EnrichedItemConverter.convert(unionItem, meta = meta))
        }
    }

    @Test
    fun `item update - meta available - send event immediately`() = runWithKafka<Unit> {
        val itemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(itemId)
        val (unionItem, meta) = EthItemConverter.convert(ethItem, itemId.blockchain).let {
            it.copy(meta = null) to it.meta!!
        }
        coEvery { testUnionMetaLoader.load(itemId) } coAnswers {
            delay(100L)
            meta
        }
        coEvery { testEthereumItemApi.getNftItemById(itemId.value) } returns ethItem.toMono()
        assertThat(itemMetaService.getAvailableMetaOrScheduleAndWait(itemId, Duration.ofMillis(3000))).isEqualTo(meta)
        Wait.waitAssert {
            val events = findItemUpdates(itemId.value)
            assertThat(events).hasSize(1)
            assertThat(events[0].value.item)
                .isEqualTo(EnrichedItemConverter.convert(unionItem, meta = meta))
        }
        metaProperties.timeoutSyncLoadingMetaMs = 2000
        itemEventService.onItemUpdated(unionItem)
        Wait.waitAssert {
            val events = findItemUpdates(itemId.value)
            assertThat(events).hasSize(2).allSatisfy {
                assertThat(it.value.item)
                    .isEqualTo(EnrichedItemConverter.convert(unionItem, meta = meta))
            }
        }
    }

}