package com.rarible.protocol.union.integration.service

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.aptos.CollectionDto
import com.rarible.protocol.dto.aptos.OwnershipDto
import com.rarible.protocol.dto.aptos.RoyaltiesDto
import com.rarible.protocol.dto.aptos.TokenDto
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.aptos.converter.AptosCollectionConverter
import com.rarible.protocol.union.integration.aptos.converter.AptosItemConverter
import com.rarible.protocol.union.integration.aptos.converter.AptosOwnershipConverter
import com.rarible.protocol.union.integration.aptos.deserializer.AptosRoyaltiesDeserializer
import com.rarible.protocol.union.integration.aptos.event.AptosCollectionEventHandler
import com.rarible.protocol.union.integration.aptos.event.AptosItemEventHandler
import com.rarible.protocol.union.integration.aptos.event.AptosOwnershipEventHandler
import com.rarible.protocol.union.integration.aptos.service.AptosUpdateService
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

class AptosUpdateServiceTest {

    private val incomingItemHandler: IncomingEventHandler<UnionItemEvent> = mockk()
    private val incomingOwnershipHandler: IncomingEventHandler<UnionOwnershipEvent> = mockk()
    private val incomingCollectionHandler: IncomingEventHandler<UnionCollectionEvent> = mockk()

    private val service = AptosUpdateService(
        itemEventHandler = AptosItemEventHandler(incomingItemHandler),
        ownershipEventHandler = AptosOwnershipEventHandler(incomingOwnershipHandler),
        collectionEventHandler = AptosCollectionEventHandler(incomingCollectionHandler)
    )

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    @BeforeEach
    internal fun setUp() {
        clearMocks(incomingItemHandler)
        clearMocks(incomingOwnershipHandler)
        clearMocks(incomingCollectionHandler)

        coEvery { incomingItemHandler.onEvent(any()) } returns Unit
        coEvery { incomingOwnershipHandler.onEvent(any()) } returns Unit
        coEvery { incomingCollectionHandler.onEvent(any()) } returns Unit

    }

    @Test
    internal fun `test item update`() {
        val module = SimpleModule()
        module.addDeserializer(RoyaltiesDto::class.java, AptosRoyaltiesDeserializer())
        mapper.registerModule(module)
        val token = ClassPathResource("json/aptos-token.json").inputStream.use {
            mapper.readValue(it, TokenDto::class.java)
        }

        runBlocking {
            val now = nowMillis()
            mockkStatic(Instant::class) {
                every { Instant.now() } returns now
                service.updateTokens(listOf(token))
                val expected = UnionItemUpdateEvent(AptosItemConverter.convert(token, BlockchainDto.APTOS))
                coVerify(exactly = 1) {
                    incomingItemHandler.onEvent(expected)
                }
            }
        }
    }

    @Test
    internal fun `test ownership update`() {
        val ownership = ClassPathResource("json/aptos-ownership.json").inputStream.use {
            mapper.readValue(it, OwnershipDto::class.java)
        }

        runBlocking {
            val now = nowMillis()
            mockkStatic(Instant::class) {
                every { Instant.now() } returns now
                service.updateOwnerships(listOf(ownership))
                val expected = UnionOwnershipUpdateEvent(AptosOwnershipConverter.convert(ownership, BlockchainDto.APTOS))
                coVerify(exactly = 1) {
                    incomingOwnershipHandler.onEvent(expected)
                }
            }
        }

    }

    @Test
    internal fun `test collection update`() {
        val collection = ClassPathResource("json/aptos-collection.json").inputStream.use {
            mapper.readValue(it, CollectionDto::class.java)
        }

        runBlocking {
            val now = nowMillis()
            mockkStatic(Instant::class) {
                every { Instant.now() } returns now
                service.updateCollections(listOf(collection))
                val expected = UnionCollectionUpdateEvent(AptosCollectionConverter.convert(collection, BlockchainDto.APTOS))
                coVerify(exactly = 1) {
                    incomingCollectionHandler.onEvent(expected)
                }
            }
        }
    }
}
