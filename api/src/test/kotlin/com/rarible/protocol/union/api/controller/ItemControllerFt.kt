package com.rarible.protocol.union.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomInt
import com.rarible.protocol.dto.NftItemRoyaltyDto
import com.rarible.protocol.dto.NftItemRoyaltyListDto
import com.rarible.protocol.dto.NftMediaDto
import com.rarible.protocol.union.api.client.ItemControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.CombinedContinuation
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.parser.ItemIdParser
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.integration.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAddress
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemMediaMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthV2OrderDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosAddress
import com.rarible.protocol.union.integration.tezos.data.randomTezosItemId
import com.rarible.protocol.union.integration.tezos.data.randomTezosItemIdFullValue
import com.rarible.protocol.union.integration.tezos.data.randomTezosMetaDto
import com.rarible.protocol.union.integration.tezos.data.randomTezosNftItemDto
import com.rarible.protocol.union.test.data.randomFlowAddress
import com.rarible.protocol.union.test.data.randomFlowItemId
import com.rarible.protocol.union.test.data.randomFlowItemIdFullValue
import com.rarible.protocol.union.test.data.randomFlowMetaDto
import com.rarible.protocol.union.test.data.randomFlowNftItemDto
import io.mockk.coEvery
import io.mockk.verify
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestTemplate
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.math.BigInteger

@FlowPreview
@IntegrationTest
class ItemControllerFt : AbstractIntegrationTest() {

    private val continuation: String? = null
    private val size = PageSize.ITEM.default

    @Autowired
    lateinit var itemControllerClient: ItemControllerApi

    @Autowired
    lateinit var enrichmentItemService: EnrichmentItemService

    @Autowired
    lateinit var ethOrderConverter: EthOrderConverter

    @Autowired
    lateinit var restTemplate: RestTemplate

    @Test
    fun `get item by id - ethereum, enriched`() = runBlocking<Unit> {
        // Enriched item
        val ethItemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(ethItemId)
        val ethUnionItem = EthItemConverter.convert(ethItem, ethItemId.blockchain)
        val ethOrder = randomEthV2OrderDto(ethItemId)
        val ethUnionOrder = ethOrderConverter.convert(ethOrder, ethItemId.blockchain)
        val ethShortItem = ShortItemConverter.convert(ethUnionItem)
            .copy(bestSellOrder = ShortOrderConverter.convert(ethUnionOrder))
        enrichmentItemService.save(ethShortItem)

        ethereumOrderControllerApiMock.mockGetById(ethOrder)
        ethereumItemControllerApiMock.mockGetNftItemById(ethItemId, ethItem)

        val result = itemControllerClient.getItemById(ethItemId.fullId()).awaitFirst()

        assertThat(result.id).isEqualTo(ethItemId)
        assertThat(result.id.blockchain).isEqualTo(BlockchainDto.ETHEREUM)
        assertThat(result.bestSellOrder!!.id).isEqualTo(ethUnionOrder.id)
    }

    @Test
    fun `get item image by id`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val ipfsUrl = "ipfs/QfgdfgajhkjkP97RAnx443626262VNFDotF9U4Jkac567457/image.png"
        val meta = randomEthItemMeta().copy(
            image = NftMediaDto(
                url = mapOf(Pair("ORIGINAL", "ipfs://$ipfsUrl")),
                meta = mapOf(Pair("ORIGINAL", randomEthItemMediaMeta("ORIGINAL")))
            )
        )

        coEvery { testEthereumItemApi.getNftItemMetaById(itemId.value) } returns Mono.just(meta)

        val response = restTemplate.getForEntity("${baseUri}/v0.1/items/${itemId.fullId()}/image", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
        assertThat(response.headers["Location"]).contains("https://rarible.mypinata.cloud/$ipfsUrl")
    }

    @Test
    fun `get item animation by id`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val ipfsUrl = "ipfs/Qmd72hpaFPnP97RAnxHKTCYb41ddVNFDotF9U4JkacsaLi/image.gif"
        val meta = randomEthItemMeta().copy(
            animation = NftMediaDto(
                url = mapOf(Pair("ORIGINAL", "ipfs://$ipfsUrl")),
                meta = mapOf(Pair("ORIGINAL", randomEthItemMediaMeta("ORIGINAL")))
            )
        )

        coEvery { testEthereumItemApi.getNftItemMetaById(itemId.value) } returns Mono.just(meta)

        val response = restTemplate.getForEntity("${baseUri}/v0.1/items/${itemId.fullId()}/animation", String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
        assertThat(response.headers["Location"]).contains("https://rarible.mypinata.cloud/$ipfsUrl")
    }

    @Test
    fun `get item by id - flow, not enriched`() = runBlocking<Unit> {
        val itemIdFull = randomFlowItemIdFullValue()
        val itemId = ItemIdParser.parseFull(itemIdFull)
        val item = randomFlowNftItemDto(itemId)

        flowItemControllerApiMock.mockGetNftItemById(itemId, item)

        val result = itemControllerClient.getItemById(itemIdFull).awaitFirst()

        assertThat(result.id.value).isEqualTo(itemId.value)
        assertThat(result.id.blockchain).isEqualTo(BlockchainDto.FLOW)
    }

    @Test
    fun `get item by id - tezos, not enriched`() = runBlocking<Unit> {
        val itemIdFull = randomTezosItemIdFullValue()
        val itemId = ItemIdParser.parseFull(itemIdFull)
        val item = randomTezosNftItemDto(itemId)

        tezosItemControllerApiMock.mockGetNftItemById(itemId, item)

        val result = itemControllerClient.getItemById(itemIdFull).awaitFirst()

        assertThat(result.id.value).isEqualTo(itemId.value)
        assertThat(result.id.blockchain).isEqualTo(BlockchainDto.TEZOS)
    }

    @Test
    fun `reset item meta by id - ethereum`() = runBlocking<Unit> {
        val itemId = randomEthItemId()

        coEvery { testEthereumItemApi.resetNftItemMetaById(itemId.value) } returns Mono.first()
        coEvery { testEthereumItemApi.getNftItemMetaById(itemId.value) } returns Mono.just(randomEthItemMeta())

        itemControllerClient.resetItemMeta(itemId.fullId()).awaitFirstOrNull()

        verify(exactly = 1) { testEthereumItemApi.resetNftItemMetaById(itemId.value) }
    }

    @Test
    fun `reset item meta by id - flow`() = runBlocking<Unit> {
        val itemId = randomFlowItemId()

        coEvery { testFlowItemApi.resetItemMeta(itemId.value) } returns Mono.first()
        coEvery { testFlowItemApi.getNftItemMetaById(itemId.value) } returns Mono.just(randomFlowMetaDto())

        itemControllerClient.resetItemMeta(itemId.fullId()).awaitFirstOrNull()

        verify(exactly = 1) { testFlowItemApi.resetItemMeta(itemId.value) }
    }

    @Test
    fun `reset item meta by id - tezos`() = runBlocking<Unit> {
        val itemId = randomTezosItemId()

        coEvery { testTezosItemApi.resetNftItemMetaById(itemId.value) } returns Mono.first()
        coEvery { testTezosItemApi.getNftItemMetaById(itemId.value) } returns Mono.just(randomTezosMetaDto())

        itemControllerClient.resetItemMeta(itemId.fullId()).awaitFirstOrNull()

        verify(exactly = 1) { testTezosItemApi.resetNftItemMetaById(itemId.value) }
    }

    @Test
    fun `get item royalties`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()

        val royalty = NftItemRoyaltyDto(randomAddress(), randomInt())

        coEvery {
            testEthereumItemApi.getNftItemRoyaltyById(ethItemId.value)
        } returns NftItemRoyaltyListDto(listOf(royalty)).toMono()

        val result = itemControllerClient.getItemRoyaltiesById(ethItemId.fullId()).awaitFirst()

        assertThat(result.royalties).hasSize(1)
        assertThat(result.royalties[0].value).isEqualTo(royalty.value)
        assertThat(result.royalties[0].account.value).isEqualTo(royalty.account.prefixed())
    }

    @Test
    fun `get items by collection - ethereum, all enriched`() = runBlocking<Unit> {
        // Enriched item
        val ethItemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(ethItemId)
        val ethUnionItem = EthItemConverter.convert(ethItem, ethItemId.blockchain)
        val ethOrder = randomEthV2OrderDto(ethItemId)
        val ethUnionOrder = ethOrderConverter.convert(ethOrder, ethItemId.blockchain)
        val ethShortItem = ShortItemConverter.convert(ethUnionItem)
            .copy(bestBidOrder = ShortOrderConverter.convert(ethUnionOrder))
        enrichmentItemService.save(ethShortItem)

        val ethCollectionId = ContractAddressConverter.convert(BlockchainDto.ETHEREUM, randomEthAddress())

        ethereumOrderControllerApiMock.mockGetByIds(ethOrder)
        ethereumItemControllerApiMock.mockGetNftOrderItemsByCollection(
            ethCollectionId.value, continuation, size, ethItem
        )

        val items = itemControllerClient.getItemsByCollection(
            ethCollectionId.fullId(), continuation, size
        ).awaitFirst()

        assertThat(items.items).hasSize(1)
        val result = items.items[0]

        assertThat(result.id).isEqualTo(ethItemId)
        assertThat(result.id.blockchain).isEqualTo(BlockchainDto.ETHEREUM)
        assertThat(result.bestBidOrder!!.id).isEqualTo(ethUnionOrder.id)
    }

    @Test
    fun `get items by collection - flow, nothing enriched`() = runBlocking<Unit> {
        val flowCollectionId = randomFlowAddress()
        val item = randomFlowNftItemDto()

        flowItemControllerApiMock.mockGetNftOrderItemsByCollection(flowCollectionId.value, continuation, size, item)

        val items = itemControllerClient.getItemsByCollection(
            flowCollectionId.fullId(), continuation, size
        ).awaitFirst()

        val result = items.items[0]
        assertThat(result.id.value).isEqualTo(item.id)
    }

    @Test
    fun `get items by collection - tezos, nothing enriched`() = runBlocking<Unit> {
        val tezosCollectionId = randomTezosAddress()
        val item = randomTezosNftItemDto()

        tezosItemControllerApiMock.mockGetNftOrderItemsByCollection(tezosCollectionId.value, continuation, size, item)

        val items = itemControllerClient.getItemsByCollection(
            tezosCollectionId.fullId(), continuation, size
        ).awaitFirst()

        val result = items.items[0]
        assertThat(result.id.value).isEqualTo(item.id)
    }

    @Test
    fun `get items by owner - ethereum, enriched partially`() = runBlocking<Unit> {
        // Enriched item
        val ethItemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(ethItemId).copy(lastUpdatedAt = nowMillis())
        val ethUnionItem = EthItemConverter.convert(ethItem, ethItemId.blockchain)
        val ethShortItem = ShortItemConverter.convert(ethUnionItem)
            .copy(totalStock = 10.toBigInteger(), sellers = 2)
        enrichmentItemService.save(ethShortItem)

        val ethOwnerId = UnionAddressConverter.convert(BlockchainDto.ETHEREUM, randomEthAddress())
        val emptyEthItem = randomEthNftItemDto().copy(lastUpdatedAt = ethItem.lastUpdatedAt!!.minusSeconds(1))

        ethereumItemControllerApiMock.mockGetNftOrderItemsByOwner(
            ethOwnerId.value, continuation, size, ethItem, emptyEthItem
        )

        val items = itemControllerClient.getItemsByOwner(
            ethOwnerId.fullId(), continuation, size
        ).awaitFirst()

        assertThat(items.items).hasSize(2)
        val enrichedResult = items.items[0]
        val emptyResult = items.items[1]

        assertThat(enrichedResult.id).isEqualTo(ethItemId)
        assertThat(enrichedResult.sellers).isEqualTo(ethShortItem.sellers)
        assertThat(enrichedResult.totalStock).isEqualTo(ethShortItem.totalStock)

        assertThat(emptyResult.id.value).isEqualTo(emptyEthItem.id)
        assertThat(emptyResult.sellers).isEqualTo(0)
        assertThat(emptyResult.totalStock).isEqualTo(BigInteger.ZERO)
    }

    @Test
    fun `get items by owner - flow, nothing enriched`() = runBlocking<Unit> {
        val flowOwnerId = randomFlowAddress()
        val flowItem = randomFlowNftItemDto()

        flowItemControllerApiMock.mockGetNftOrderItemsByOwner(
            flowOwnerId.value, continuation, size, flowItem
        )

        val items = itemControllerClient.getItemsByOwner(
            flowOwnerId.fullId(), continuation, size
        ).awaitFirst()

        val result = items.items[0]
        assertThat(result.id.value).isEqualTo(flowItem.id)
    }

    @Test
    fun `get items by owner - tezos, nothing enriched`() = runBlocking<Unit> {
        val tezosOwnerId = randomTezosAddress()
        val tezosItem = randomTezosNftItemDto()

        tezosItemControllerApiMock.mockGetNftOrderItemsByOwner(
            tezosOwnerId.value, continuation, size, tezosItem
        )

        val items = itemControllerClient.getItemsByOwner(
            tezosOwnerId.fullId(), continuation, size
        ).awaitFirst()

        val result = items.items[0]
        assertThat(result.id.value).isEqualTo(tezosItem.id)
    }

    @Test
    fun `get items by creator - ethereum, nothing found`() = runBlocking<Unit> {
        val ethCreatorId = UnionAddressConverter.convert(BlockchainDto.ETHEREUM, randomEthAddress())

        ethereumItemControllerApiMock.mockGetNftOrderItemsByCreator(
            ethCreatorId.value, continuation, size
        )

        val items = itemControllerClient.getItemsByCreator(
            ethCreatorId.fullId(), continuation, size
        ).awaitFirst()

        assertThat(items.items).hasSize(0)
    }

    @Test
    fun `get items by creator - flow, nothing enriched`() = runBlocking<Unit> {
        val flowCreatorId = randomFlowAddress()
        val flowItem = randomFlowNftItemDto()

        flowItemControllerApiMock.mockGetNftOrderItemsByCreator(
            flowCreatorId.value, continuation, size, flowItem
        )

        val items = itemControllerClient.getItemsByCreator(
            flowCreatorId.fullId(), continuation, size
        ).awaitFirst()

        val result = items.items[0]
        assertThat(result.id.value).isEqualTo(flowItem.id)
    }

    @Test
    fun `get items by creator - tezos, nothing enriched`() = runBlocking<Unit> {
        val tezosCreatorId = randomTezosAddress()
        val tezosItem = randomTezosNftItemDto()

        tezosItemControllerApiMock.mockGetNftOrderItemsByCreator(
            tezosCreatorId.value, continuation, size, tezosItem
        )

        val items = itemControllerClient.getItemsByCreator(
            tezosCreatorId.fullId(), continuation, size
        ).awaitFirst()

        val result = items.items[0]
        assertThat(result.id.value).isEqualTo(tezosItem.id)
    }

    @Test
    fun `get all items - trimmed to size`() = runBlocking<Unit> {
        val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.FLOW)
        val now = nowMillis()

        val ethList = listOf(
            randomEthNftItemDto().copy(lastUpdatedAt = now),
            randomEthNftItemDto().copy(lastUpdatedAt = now.minusSeconds(10))
        )

        val flowList = listOf(
            randomFlowNftItemDto().copy(lastUpdatedAt = now),
            randomFlowNftItemDto().copy(lastUpdatedAt = now.minusSeconds(10))
        )

        val ethContinuation = "${now.toEpochMilli()}_${ethList.first().id}"
        val flowContinuation = "${now.toEpochMilli()}_${flowList.first().id}"
        val cursorArg = CombinedContinuation(
            mapOf(
                BlockchainDto.ETHEREUM.toString() to ethContinuation,
                BlockchainDto.FLOW.toString() to flowContinuation
            )
        )
        val showDeleted = true
        val size = 3
        val lastUpdatedFrom = nowMillis().minusSeconds(120).toEpochMilli()
        val lastUpdatedTo = nowMillis().plusSeconds(120).toEpochMilli()

        flowItemControllerApiMock.mockGetNftAllItems(
            flowContinuation, size, showDeleted, lastUpdatedFrom, lastUpdatedTo, *flowList.toTypedArray()
        )

        ethereumItemControllerApiMock.mockGetNftAllItems(
            ethContinuation, size, showDeleted, lastUpdatedFrom, lastUpdatedTo, *ethList.toTypedArray()
        )

        val items = itemControllerClient.getAllItems(
            blockchains, cursorArg.toString(), size, showDeleted, lastUpdatedFrom, lastUpdatedTo
        ).awaitFirst()

        assertThat(items.items).hasSize(3)
        assertThat(items.total).isEqualTo(4)
        assertThat(items.continuation).isNotNull()
    }
}
