package com.rarible.protocol.union.integration.tezos.service

import com.rarible.protocol.tezos.api.client.NftItemControllerApi
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupIntegrationProperties
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupIntegrationProperties.TzktProperties
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemServiceImpl
import com.rarible.tzkt.client.TokenClient
import com.rarible.tzkt.model.Alias
import com.rarible.tzkt.model.Page
import com.rarible.tzkt.model.Token
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant.now
import java.time.ZoneOffset
import java.time.Instant.now
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TzktItemServiceTest {

    private val itemControllerApi: NftItemControllerApi = mockk()
    private val tokenClient: TokenClient = mockk()
    private val dipdupProps: DipDupIntegrationProperties = mockk()

    private val tzktItemService = TzktItemServiceImpl(tokenClient, dipdupProps)
    private val service = TezosItemService(itemControllerApi, tzktItemService)

    @BeforeEach
    fun beforeEach() {
        clearMocks(itemControllerApi)
        clearMocks(tokenClient)
    }

    @Test
    fun `get tzkt item by id`() = runBlocking<Unit> {
        val itemId = "test:123"
        val tzktToken = tzktToken(itemId)
        coEvery { tokenClient.token(itemId) } returns tzktToken

        val item = service.getItemById(itemId)
        assertThat(item.id).isEqualTo(ItemIdDto(BlockchainDto.TEZOS, itemId))
        assertThat(item.deleted).isFalse()
    }

    @Test
    fun `get tzkt items by ids`() = runBlocking<Unit> {
        val itemId = "test:123"
        val tzktToken = tzktToken(itemId)
        coEvery { tokenClient.tokens(listOf(itemId)) } returns listOf(tzktToken)

        val items = service.getItemsByIds(listOf(itemId))
        assertThat(items).hasSize(1)
    }

    @Test
    fun `get tzkt itemsAll`() = runBlocking<Unit> {
        val itemId = "test:123"
        val tzktToken = tzktToken(itemId)
        val continuation = "continuation"
        coEvery { tokenClient.allTokensByLastUpdate(100, null, true) } returns Page(listOf(tzktToken), continuation)

        val page = service.getAllItems(
            continuation = null,
            size = 100,
            showDeleted = true,
            lastUpdatedFrom = null,
            lastUpdatedTo = null
        )
        assertThat(page.entities).hasSize(1)
        assertThat(page.continuation).isEqualTo(continuation)
    }

    @Nested
    inner class isNftTest {
        @BeforeEach
        fun beforeEach() {
            coEvery { dipdupProps.fungibleContracts } returns emptySet()
        }

        @Test
        fun `should return true is artifactUrl is preset in meta`() = runBlocking<Unit> {
            val itemId = "test:123"
            coEvery { dipdupProps.tzktProperties } returns TzktProperties()
            coEvery { tokenClient.token(itemId) } returns tzktToken(itemId).copy(
                metadata = mapOf("artifactUri" to Object())
            )

            assertTrue(tzktItemService.isNft(itemId))
        }

        @Test
        fun `should return false is artifactUrl is empty`() = runBlocking<Unit> {
            val itemId = "test:123"
            coEvery { dipdupProps.tzktProperties } returns TzktProperties(retryAttempts = 1)
            coEvery { tokenClient.token(itemId) } returns tzktToken(itemId)

            assertFalse(tzktItemService.isNft(itemId))
        }

        @Test
        fun `should make only 1 attempt for an 'old' nft`() = runBlocking<Unit> {
            val itemId = "test:123"
            coEvery { dipdupProps.tzktProperties } returns TzktProperties()
            coEvery { tokenClient.token(itemId) } returns tzktToken(itemId).copy(
                lastTime = now().atOffset(ZoneOffset.UTC).minusDays(TzktProperties().ignorePeriod + 1)
            )

            assertFalse(tzktItemService.isNft(itemId))
        }

        @Test
        fun `should return true after the second attempt`() = runBlocking<Unit> {
            val itemId = "test:123"
            coEvery { dipdupProps.tzktProperties } returns TzktProperties(retryAttempts = 5, retryDelay = 1)
            coEvery { tokenClient.token(itemId) } returns tzktToken(itemId) andThen tzktToken(itemId).copy(
                metadata = mapOf("artifactUri" to Object())
            )

            assertTrue(tzktItemService.isNft(itemId))
            coVerify(exactly = 2) { tokenClient.token(itemId) }
        }

        @Test
        fun `should skip fungible token`() = runBlocking<Unit> {
            val itemId = "test:123"
            coEvery { dipdupProps.fungibleContracts } returns setOf("test")

            assertFalse(tzktItemService.isNft(itemId))
            coVerify(exactly = 0) { tokenClient.token(itemId) }
        }
    }

    fun tzktToken(itemId: String) = Token(
        id = 1,
        contract = Alias(
            alias = "test name",
            address = "test"
        ),
        tokenId = "123",
        firstTime = now().atOffset(ZoneOffset.UTC),
        lastTime = now().atOffset(ZoneOffset.UTC),
        totalSupply = "1",
        transfersCount = 1,
        balancesCount = 1,
        holdersCount = 1
    )
}
