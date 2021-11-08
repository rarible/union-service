package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.NftOwnershipsDto
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOwnershipConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toMono

class EthOwnershipServiceTest {

    private val ownershipControllerApi: NftOwnershipControllerApi = mockk()
    private val service = EthOwnershipService(BlockchainDto.ETHEREUM, ownershipControllerApi)

    @Test
    fun `ethereum get all ownerships`() = runBlocking<Unit> {
        val ownershipId = randomEthOwnershipId()
        val ownership = randomEthOwnershipDto(ownershipId)

        val continuation = randomString()
        val size = randomInt()

        val expected = EthOwnershipConverter.convert(ownership, BlockchainDto.ETHEREUM)

        coEvery {
            ownershipControllerApi.getNftAllOwnerships(continuation, size)
        } returns NftOwnershipsDto(500, "abc", listOf(ownership)).toMono()

        val result = service.getAllOwnerships(continuation, size)

        assertThat(result.total).isEqualTo(500)
        assertThat(result.continuation).isEqualTo("abc")
        assertThat(result.entities).hasSize(1)
        assertThat(result.entities[0]).isEqualTo(expected)
    }

    @Test
    fun `ethereum get ownership by id`() = runBlocking<Unit> {
        val ownershipId = randomEthOwnershipId()
        val ownership = randomEthOwnershipDto(ownershipId)

        val expected = EthOwnershipConverter.convert(ownership, BlockchainDto.ETHEREUM)

        coEvery { ownershipControllerApi.getNftOwnershipById(ownership.id) } returns ownership.toMono()

        val result = service.getOwnershipById(ownershipId.value)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `ethereum get ownerships by item`() = runBlocking<Unit> {
        val ownershipId = randomEthOwnershipId()
        val ownership = randomEthOwnershipDto(ownershipId)

        val continuation = randomString()
        val size = randomInt()

        val expected = EthOwnershipConverter.convert(ownership, BlockchainDto.ETHEREUM)

        coEvery {
            ownershipControllerApi.getNftOwnershipsByItem(
                ownershipId.token.value,
                ownershipId.tokenId.toString(),
                continuation,
                size
            )
        } returns NftOwnershipsDto(100, "abc", listOf(ownership)).toMono()

        val result = service.getOwnershipsByItem(
            EthConverter.convert(ownership.contract),
            ownership.tokenId.toString(),
            continuation,
            size
        )

        assertThat(result.total).isEqualTo(100)
        assertThat(result.continuation).isEqualTo("abc")
        assertThat(result.entities).hasSize(1)
        assertThat(result.entities[0]).isEqualTo(expected)
    }
}