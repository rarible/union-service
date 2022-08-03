package com.rarible.protocol.union.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.NftItemIdsDto
import com.rarible.protocol.union.api.client.ItemControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.converter.EsOwnershipConverter
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.parser.OwnershipIdParser
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.test.data.randomEsItem
import com.rarible.protocol.union.integration.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOwnershipConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAddress
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthV2OrderDto
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import randomEsOwnership
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import java.math.BigInteger
import java.time.Instant

@FlowPreview
@IntegrationTest
@TestPropertySource(properties = ["common.feature-flags.enableItemQueriesToElasticSearch=true"])
class ItemsControllerElasticFt : AbstractIntegrationTest() {

    private val continuation: String? = null
    private val size = PageSize.ITEM.default

    @Autowired
    lateinit var itemControllerClient: ItemControllerApi

    @Autowired
    private lateinit var repository: EsItemRepository

    @Autowired
    private lateinit var esOwnershipRepository: EsOwnershipRepository

    @Autowired
    lateinit var ethOrderConverter: EthOrderConverter

    @Autowired
    lateinit var enrichmentItemService: EnrichmentItemService

    @Autowired
    private lateinit var ownershipRepository: EsOwnershipRepository

    @Autowired
    private lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    @BeforeEach
    fun setUp() = runBlocking {
        elasticsearchTestBootstrapper.bootstrap()
    }

    @Test
    fun `get all activities`() = runBlocking<Unit> {

        val blockchains = listOf(
            BlockchainDto.ETHEREUM,
            BlockchainDto.POLYGON,
            BlockchainDto.FLOW,
            BlockchainDto.SOLANA,
            BlockchainDto.TEZOS
        )
        val now = nowMillis()
        val contract = Address.ONE().toString()
        val ethItemId1 = ItemIdDto(BlockchainDto.ETHEREUM, contract, BigInteger.valueOf(1))
        val ethItem1 = randomEthNftItemDto(ethItemId1)
        val ethEsItem1 = randomEsItem().copy(
            itemId = ethItemId1.toString(),
            blockchain = BlockchainDto.ETHEREUM,
            lastUpdatedAt = now.plusMillis(-1)
        )
        repository.save(ethEsItem1)
        val ethItemId2 = ItemIdDto(BlockchainDto.ETHEREUM, contract, BigInteger.valueOf(2))
        val ethItem2 = randomEthNftItemDto(ethItemId2)
        val ethEsItem2 = randomEsItem().copy(
            itemId = ethItemId2.toString(),
            blockchain = BlockchainDto.ETHEREUM,
            lastUpdatedAt = now.plusMillis(-2)
        )
        repository.save(ethEsItem2)
        val ethItemId3 = ItemIdDto(BlockchainDto.ETHEREUM, contract, BigInteger.valueOf(3))
        val ethItem3 = randomEthNftItemDto(ethItemId3)
        val ethEsItem3 = randomEsItem().copy(
            itemId = ethItemId3.toString(),
            blockchain = BlockchainDto.ETHEREUM,
            lastUpdatedAt = now.plusMillis(-3)
        )
        repository.save(ethEsItem3)
        val ethItemId4 = ItemIdDto(BlockchainDto.ETHEREUM, contract, BigInteger.valueOf(4))
        val ethItem4 = randomEthNftItemDto(ethItemId4)
        val ethEsItem4 = randomEsItem().copy(
            itemId = ethItemId4.toString(),
            blockchain = BlockchainDto.ETHEREUM,
            lastUpdatedAt = now.plusMillis(-4)
        )
        repository.save(ethEsItem4)
        val ethItemId5 = ItemIdDto(BlockchainDto.ETHEREUM, contract, BigInteger.valueOf(5))
        val ethItem5 = randomEthNftItemDto(ethItemId5)
        val ethEsItem5 = randomEsItem().copy(
            itemId = ethItemId5.toString(),
            blockchain = BlockchainDto.ETHEREUM,
            lastUpdatedAt = now.plusMillis(-5)
        )
        repository.save(ethEsItem5)
        val ethItemId6 = ItemIdDto(BlockchainDto.ETHEREUM, contract, BigInteger.valueOf(6))
        val ethItem6 = randomEthNftItemDto(ethItemId6)
        val ethEsItem6 = randomEsItem().copy(
            itemId = ethItemId6.toString(),
            blockchain = BlockchainDto.ETHEREUM,
            lastUpdatedAt = now.plusMillis(-6)
        )
        repository.save(ethEsItem6)

        val polygonItemId1 = ItemIdDto(BlockchainDto.POLYGON, contract, BigInteger.valueOf(1))
        val polygonItem1 = randomEthNftItemDto(polygonItemId1)
        val polygonEsItem1 = randomEsItem().copy(
            itemId = polygonItemId1.toString(),
            blockchain = BlockchainDto.POLYGON,
            lastUpdatedAt = now.plusMillis(-7)
        )
        repository.save(polygonEsItem1)
        val polygonItemId2 = ItemIdDto(BlockchainDto.POLYGON, contract, BigInteger.valueOf(2))
        val polygonItem2 = randomEthNftItemDto(polygonItemId2)
        val polygonEsItem2 = randomEsItem().copy(
            itemId = polygonItemId2.toString(),
            blockchain = BlockchainDto.POLYGON,
            lastUpdatedAt = now.plusMillis(-8)
        )
        repository.save(polygonEsItem2)
        val polygonItemId3 = ItemIdDto(BlockchainDto.POLYGON, contract, BigInteger.valueOf(3))
        val polygonItem3 = randomEthNftItemDto(polygonItemId3)
        val polygonEsItem3 = randomEsItem().copy(
            itemId = polygonItemId3.toString(),
            blockchain = BlockchainDto.POLYGON,
            lastUpdatedAt = now.plusMillis(-9)
        )
        repository.save(polygonEsItem3)
        val polygonItemId4 = ItemIdDto(BlockchainDto.POLYGON, contract, BigInteger.valueOf(4))
        val polygonItem4 = randomEthNftItemDto(polygonItemId4)
        val polygonEsItem4 = randomEsItem().copy(
            itemId = polygonItemId4.toString(),
            blockchain = BlockchainDto.POLYGON,
            lastUpdatedAt = now.plusMillis(-10)
        )
        repository.save(polygonEsItem4)
        val polygonItemId5 = ItemIdDto(BlockchainDto.POLYGON, contract, BigInteger.valueOf(5))
        val polygonItem5 = randomEthNftItemDto(polygonItemId5)
        val polygonEsItem5 = randomEsItem().copy(
            itemId = polygonItemId5.toString(),
            blockchain = BlockchainDto.POLYGON,
            lastUpdatedAt = now.plusMillis(-11)
        )
        repository.save(polygonEsItem5)
        val polygonItemId6 = ItemIdDto(BlockchainDto.POLYGON, contract, BigInteger.valueOf(6))
        val polygonItem6 = randomEthNftItemDto(polygonItemId6)
        val polygonEsItem6 = randomEsItem().copy(
            itemId = polygonItemId6.toString(),
            blockchain = BlockchainDto.POLYGON,
            lastUpdatedAt = now.plusMillis(-12)
        )
        repository.save(polygonEsItem6)

        // Since all items types specified in request, all of existing clients should be requested
        coEvery {
            testEthereumItemApi.getNftItemsByIds(
                NftItemIdsDto(
                    listOf(
                        ethItem1.id,
                        ethItem2.id,
                        ethItem3.id
                    )
                )
            )
        } returns listOf(ethItem1, ethItem2, ethItem3).toFlux()

        coEvery {
            testEthereumItemApi.getNftItemsByIds(
                NftItemIdsDto(
                    listOf(
                        ethItem4.id,
                        ethItem5.id,
                        ethItem6.id
                    )
                )
            )
        } returns listOf(ethItem4, ethItem5, ethItem6).toFlux()

        coEvery {
            testPolygonItemApi.getNftItemsByIds(
                NftItemIdsDto(
                    listOf(
                        polygonItem1.id,
                        polygonItem2.id,
                        polygonItem3.id,
                    )
                )
            )
        } returns listOf(polygonItem1, polygonItem2, polygonItem3).toFlux()

        coEvery {
            testPolygonItemApi.getNftItemsByIds(
                NftItemIdsDto(
                    listOf(
                        polygonItem4.id,
                        polygonItem5.id,
                        polygonItem6.id,
                    )
                )
            )
        } returns listOf(polygonItem4, polygonItem5, polygonItem6).toFlux()

        val showDeleted: Boolean? = null
        val size = 3
        val lastUpdatedFrom = nowMillis().minusSeconds(120).toEpochMilli()
        val lastUpdatedTo = nowMillis().plusSeconds(120).toEpochMilli()

        var result = itemControllerClient.getAllItems(
            blockchains, null, size, showDeleted, lastUpdatedFrom, lastUpdatedTo
        ).awaitFirst()

        assertThat(result.items).hasSize(3)
        assertThat(result.items.map { it.id.toString() }).containsAll(
            listOf(
                ethItemId1.toString(),
                ethItemId2.toString(),
                ethItemId3.toString()
            )
        )
        assertThat(result.continuation).isNotNull()

        result = itemControllerClient.getAllItems(
            blockchains, result.continuation, size, showDeleted, lastUpdatedFrom, lastUpdatedTo
        ).awaitFirst()

        assertThat(result.items).hasSize(3)
        assertThat(result.items.map { it.id.toString() }).containsAll(
            listOf(
                ethItemId4.toString(),
                ethItemId5.toString(),
                ethItemId6.toString()
            )
        )
        assertThat(result.continuation).isNotNull()

        result = itemControllerClient.getAllItems(
            blockchains, result.continuation, size, showDeleted, lastUpdatedFrom, lastUpdatedTo
        ).awaitFirst()

        assertThat(result.items).hasSize(3)
        assertThat(result.items.map { it.id.toString() }).containsAll(
            listOf(
                polygonItemId1.toString(),
                polygonItemId2.toString(),
                polygonItemId3.toString()
            )
        )
        assertThat(result.continuation).isNotNull()

        result = itemControllerClient.getAllItems(
            blockchains, result.continuation, size, showDeleted, lastUpdatedFrom, lastUpdatedTo
        ).awaitFirst()

        assertThat(result.items).hasSize(3)
        assertThat(result.items.map { it.id.toString() }).containsAll(
            listOf(
                polygonItemId4.toString(),
                polygonItemId5.toString(),
                polygonItemId6.toString()
            )
        )
        assertThat(result.continuation).isNotNull()

        result = itemControllerClient.getAllItems(
            blockchains, result.continuation, size, showDeleted, lastUpdatedFrom, lastUpdatedTo
        ).awaitFirst()

        assertThat(result.items).hasSize(0)
        assertThat(result.continuation).isNull()
    }

    @Test
    fun `get items by collection - ethereum, all enriched`() = runBlocking<Unit> {
        val ethCollectionId = CollectionIdDto(BlockchainDto.ETHEREUM, randomEthAddress())
        // Enriched item
        val ethItemId1 = ItemIdDto(BlockchainDto.ETHEREUM, ethCollectionId.value, BigInteger.valueOf(1))
        val ethEsItem1 = randomEsItem().copy(
            itemId = ethItemId1.toString(),
            collection = ethCollectionId.fullId(),
            blockchain = BlockchainDto.ETHEREUM
        )
        repository.save(ethEsItem1)
        val ethItemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(ethItemId1)
        val ethUnionItem = EthItemConverter.convert(ethItem, ethItemId.blockchain)
        val ethOrder = randomEthV2OrderDto(ethItemId)
        val ethUnionOrder = ethOrderConverter.convert(ethOrder, ethItemId.blockchain)
        val ethShortItem = ShortItemConverter.convert(ethUnionItem)
            .copy(bestBidOrder = ShortOrderConverter.convert(ethUnionOrder))
        enrichmentItemService.save(ethShortItem)

        coEvery {
            testEthereumItemApi.getNftItemsByIds(
                NftItemIdsDto(listOf(ethItemId1.value))
            )
        } returns listOf(ethItem).toFlux()

        ethereumOrderControllerApiMock.mockGetByIds(ethOrder)
        val items = itemControllerClient.getItemsByCollection(
            ethCollectionId.fullId(), continuation, size
        ).awaitFirst()

        assertThat(items.items).hasSize(1)
        val result = items.items[0]

        assertThat(result.id.value).isEqualTo(ethItem.id)
        assertThat(result.id.blockchain).isEqualTo(BlockchainDto.ETHEREUM)
        assertThat(result.bestBidOrder!!.id).isEqualTo(ethUnionOrder.id)
    }

    @Test
    @Disabled("In another PR it will be fixed")
    fun `get items by owner - ethereum, all enriched`() = runBlocking<Unit> {
        val ethCollectionId = CollectionIdDto(BlockchainDto.ETHEREUM, randomEthAddress())
        // Enriched item
        val ethItemId1 = ItemIdDto(BlockchainDto.ETHEREUM, ethCollectionId.value, BigInteger.valueOf(1))
        val esOwnership = randomEsOwnership().copy(
            itemId = ethItemId1.toString(),
            blockchain = BlockchainDto.ETHEREUM
        )
        esOwnershipRepository.saveAll(listOf(esOwnership))
        val ethItemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(ethItemId)
        val ethUnionItem = EthItemConverter.convert(ethItem, ethItemId.blockchain)
        val ethOrder = randomEthV2OrderDto(ethItemId)
        val ethUnionOrder = ethOrderConverter.convert(ethOrder, ethItemId.blockchain)
        val ethShortItem = ShortItemConverter.convert(ethUnionItem)
            .copy(bestBidOrder = ShortOrderConverter.convert(ethUnionOrder))
        enrichmentItemService.save(ethShortItem)

        coEvery {
            testEthereumItemApi.getNftItemsByIds(
                NftItemIdsDto(listOf(ethItemId1.fullId()))
            )
        } returns listOf(ethItem).toFlux()

        ethereumOrderControllerApiMock.mockGetByIds(ethOrder)
        val items = itemControllerClient.getItemsByOwner(
            esOwnership.owner, listOf(BlockchainDto.ETHEREUM), continuation, size
        ).awaitFirst()

        assertThat(items.items).hasSize(1)
        val result = items.items[0]

        assertThat(result.id).isEqualTo(ethItemId)
        assertThat(result.id.blockchain).isEqualTo(BlockchainDto.ETHEREUM)
        assertThat(result.bestBidOrder!!.id).isEqualTo(ethUnionOrder.id)
    }

    @Test
    fun `get items by creator - ethereum, all enriched`() = runBlocking<Unit> {
        // given
        val ethCollectionId = CollectionIdDto(BlockchainDto.ETHEREUM, randomEthAddress())
        val creatorId = CollectionIdDto(BlockchainDto.ETHEREUM, randomEthAddress())
        // Enriched item
        val ethItemId1 = ItemIdDto(BlockchainDto.ETHEREUM, ethCollectionId.value, BigInteger.valueOf(1))
        val ethEsItem1 = randomEsItem().copy(
            itemId = ethItemId1.toString(),
            creators = listOf(creatorId.fullId()),
            blockchain = BlockchainDto.ETHEREUM
        )
        repository.save(ethEsItem1)
        val ethItemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(ethItemId1)
        val ethUnionItem = EthItemConverter.convert(ethItem, ethItemId.blockchain)
        val ethOrder = randomEthV2OrderDto(ethItemId)
        val ethUnionOrder = ethOrderConverter.convert(ethOrder, ethItemId.blockchain)
        val ethShortItem = ShortItemConverter.convert(ethUnionItem)
            .copy(bestBidOrder = ShortOrderConverter.convert(ethUnionOrder))
        enrichmentItemService.save(ethShortItem)

        coEvery {
            testEthereumItemApi.getNftItemsByIds(
                NftItemIdsDto(listOf(ethItemId1.value))
            )
        } returns listOf(ethItem).toFlux()
        ethereumOrderControllerApiMock.mockGetByIds(ethOrder)

        // when
        val items = itemControllerClient.getItemsByCreator(
            creatorId.fullId(), listOf(BlockchainDto.ETHEREUM), continuation, size
        ).awaitFirst()

        assertThat(items.items).hasSize(1)
        val result = items.items[0]

        assertThat(result.id).isEqualTo(ethItemId1)
        assertThat(result.id.blockchain).isEqualTo(BlockchainDto.ETHEREUM)
        assertThat(result.bestBidOrder!!.id).isEqualTo(ethUnionOrder.id)
    }

    @Test
    fun `get items by owner with ownerships - ethereum`() = runBlocking<Unit> {
        // given
        val ethItemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(ethItemId)

        val ethOwnership = randomEthOwnershipDto(ethItemId).copy(value = BigInteger.ONE)
        val ethOwnerId = UnionAddressConverter.convert(BlockchainDto.ETHEREUM, ethOwnership.owner.prefixed())
        val ethOwnershipId = OwnershipIdDto(BlockchainDto.ETHEREUM, ethItemId.value, ethOwnerId)
        val ethUnionOwnership = EthOwnershipConverter.convert(ethOwnership, ethItemId.blockchain)
        val esOwnership = EsOwnershipConverter.convert(ethUnionOwnership)
        ethereumOwnershipControllerApiMock.mockGetNftOwnershipByIds(
            listOf(OwnershipIdParser.parseFull(esOwnership.ownershipId).value),
            listOf(ethOwnership)
        )
        esOwnershipRepository.save(esOwnership)
        val esItem = randomEsItem().copy(
            itemId = ethItemId.toString(),
            blockchain = BlockchainDto.ETHEREUM
        )
        repository.save(esItem)
        coEvery {
            testEthereumItemApi.getNftItemsByIds(
                NftItemIdsDto(listOf(ethItemId.value))
            )
        } returns listOf(ethItem).toFlux()

        // when
        val actual = itemControllerClient.getItemsByOwnerWithOwnership(
            ethOwnerId.fullId(), continuation, size
        ).awaitFirst()

        // then
        assertThat(actual.items).hasSize(1)
        assertThat(actual.continuation).isNotNull()
        assertThat(actual.items.first().item.id).isEqualTo(ethItemId)
        assertThat(actual.items.first().ownership.id).isEqualTo(ethOwnershipId)
    }
}
