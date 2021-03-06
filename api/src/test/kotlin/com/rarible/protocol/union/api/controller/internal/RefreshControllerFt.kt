package com.rarible.protocol.union.api.controller.internal

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.test.data.randomBigInt
import com.rarible.protocol.dto.ActivitySortDto
import com.rarible.protocol.dto.NftActivityFilterByItemDto
import com.rarible.protocol.dto.OrderActivityFilterByItemDto
import com.rarible.protocol.dto.OrderActivityMatchDto
import com.rarible.protocol.dto.OrdersPaginationDto
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CollectionUpdateEventDto
import com.rarible.protocol.union.dto.ItemDeleteEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemUpdateEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.OwnershipSourceDto
import com.rarible.protocol.union.dto.OwnershipUpdateEventDto
import com.rarible.protocol.union.dto.ext
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.converter.ShortOwnershipConverter
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.util.bidCurrencyId
import com.rarible.protocol.union.enrichment.util.sellCurrencyId
import com.rarible.protocol.union.integration.ethereum.converter.EthAuctionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOwnershipConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc1155
import com.rarible.protocol.union.integration.ethereum.data.randomEthAssetErc20
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthBidOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionAsset
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemMintActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthOrderActivityMatch
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthSellOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthV2OrderDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import java.math.BigInteger

@IntegrationTest
@ExperimentalCoroutinesApi
class RefreshControllerFt : AbstractIntegrationTest() {

    @Autowired
    lateinit var ethOrderConverter: EthOrderConverter

    @Autowired
    lateinit var ethAuctionConverter: EthAuctionConverter

    @Autowired
    lateinit var enrichmentItemService: EnrichmentItemService

    @Autowired
    lateinit var enrichmentOwnershipService: EnrichmentOwnershipService

    private val origin = "0xWhitelabel"
    private val ethOriginCollection = "0xf3348949db80297c78ec17d19611c263fc61f988" // from application.yaml

    @Test
    fun `reconcile item - full`() = runBlocking<Unit> {
        val itemId = ItemIdDto(BlockchainDto.ETHEREUM, ethOriginCollection, randomBigInt())
        val ethItem = randomEthNftItemDto(itemId)
        val unionItem = EthItemConverter.convert(ethItem, itemId.blockchain)
        val shortItem = ShortItemConverter.convert(unionItem)

        val ethBestSell = randomEthSellOrderDto(itemId)
        val unionBestSell = ethOrderConverter.convert(ethBestSell, itemId.blockchain)
        val shortBestSell = ShortOrderConverter.convert(unionBestSell)

        val ethBestBid = randomEthBidOrderDto(itemId)
        val unionBestBid = ethOrderConverter.convert(ethBestBid, itemId.blockchain)
        val shortBestBid = ShortOrderConverter.convert(unionBestBid)

        val bidCurrency = unionBestBid.bidCurrencyId
        val sellCurrency = unionBestSell.sellCurrencyId

        val ethOriginBestSell = randomEthSellOrderDto(itemId).copy(take = ethBestSell.take)
        val shortOriginBestSell = ShortOrderConverter.convert(unionBestSell)

        val ethOriginBestBid = randomEthBidOrderDto(itemId).copy(make = ethBestBid.make)
        val shorOriginBestBid = ShortOrderConverter.convert(unionBestBid)

        val ethAuction = randomEthAuctionDto(itemId)
        val auction = ethAuctionConverter.convert(ethAuction, BlockchainDto.ETHEREUM)

        // Fully auctioned ownership, should not be saved, but disguised event is expected for it
        val ethAuctionedOwnershipId = itemId.toOwnership(auction.seller.value)
        val auctionOwnershipId = itemId.toOwnership(auction.contract.value)
        val auctionOwnership = randomEthOwnershipDto(auctionOwnershipId)

        // Free ownership - should be reconciled in regular way
        val ethFreeOwnershipId = itemId.toOwnership(auction.seller.value)
        val ethOwnership = randomEthOwnershipDto(ethFreeOwnershipId)
        val unionOwnership = EthOwnershipConverter.convert(ethOwnership, ethFreeOwnershipId.blockchain)
        val shortOwnership = ShortOwnershipConverter.convert(unionOwnership)

        // Last sell activity for item
        val swapDto = randomEthOrderActivityMatch()
        val activity = swapDto.copy(left = swapDto.left.copy(asset = randomEthAssetErc1155(itemId)))

        ethereumOwnershipControllerApiMock.mockGetNftOwnershipsByItem(itemId, null, 1000, ethOwnership)
        ethereumOwnershipControllerApiMock.mockGetNftOwnershipById(auctionOwnershipId, auctionOwnership)
        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(itemId, listOf(ethAuction))
        ethereumItemControllerApiMock.mockGetNftItemById(itemId, ethItem)
        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(itemId, ethBestSell.take.assetType)
        // Best sell for Item
        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(itemId, sellCurrency, ethBestSell)
        // Same best sell for free Ownership
        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(ethFreeOwnershipId, sellCurrency, ethBestSell)
        // Best sell for Item's origin
        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(
            itemId, sellCurrency, origin, ethOriginBestSell
        )
        // Same best sell for Ownership origin
        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(
            ethFreeOwnershipId, sellCurrency, origin, ethOriginBestSell
        )

        val mintActivity = randomEthItemMintActivity().copy(owner = Address.apply(ethFreeOwnershipId.owner.value))
        ethereumActivityControllerApiMock.mockGetNftActivitiesByItem(
            itemId,
            listOf(NftActivityFilterByItemDto.Types.MINT),
            1,
            null,
            ActivitySortDto.LATEST_FIRST,
            mintActivity
        )

        ethereumOrderControllerApiMock.mockGetCurrenciesByBidOrdersOfItem(itemId, ethBestBid.make.assetType)
        // Item best bid
        ethereumOrderControllerApiMock.mockGetOrderBidsByItemAndByStatus(itemId, bidCurrency, ethBestBid)
        // Item's origin best bid
        ethereumOrderControllerApiMock.mockGetOrderBidsByItemAndByStatus(itemId, bidCurrency, origin, ethOriginBestBid)

        mockLastSellActivity(itemId, activity)

        val uri = "$baseUri/v0.1/refresh/item/${itemId.fullId()}/reconcile?full=true"
        val result = testRestTemplate.postForEntity(uri, null, ItemEventDto::class.java).body!!
        val reconciled = (result as ItemUpdateEventDto).item
        val savedShortItem = enrichmentItemService.get(shortItem.id)!!
        val savedShortOwnership = enrichmentOwnershipService.get(shortOwnership.id)!!

        val itemOriginOrders = savedShortItem.originOrders.toList()[0]
        val ownershipOriginOrders = savedShortOwnership.originOrders.toList()[0]

        assertThat(savedShortItem.bestSellOrder!!.id).isEqualTo(shortBestSell.id)
        assertThat(savedShortItem.bestBidOrder!!.id).isEqualTo(shortBestBid.id)
        assertThat(savedShortItem.bestSellOrders[unionBestSell.sellCurrencyId]!!.id).isEqualTo(shortBestSell.id)
        assertThat(savedShortItem.bestBidOrders[unionBestBid.bidCurrencyId]!!.id).isEqualTo(shortBestBid.id)
        assertThat(savedShortItem.auctions).isEqualTo(setOf(auction.id))
        assertThat(savedShortItem.lastSale!!.date).isEqualTo(activity.date)

        assertThat(itemOriginOrders.bestSellOrder!!.id).isEqualTo(shortOriginBestSell.id)
        assertThat(itemOriginOrders.bestBidOrder!!.id).isEqualTo(shorOriginBestBid.id)

        assertThat(savedShortOwnership.source).isEqualTo(OwnershipSourceDto.MINT)
        assertThat(savedShortOwnership.bestSellOrder!!.id).isEqualTo(shortBestSell.id)
        assertThat(savedShortOwnership.bestSellOrders[unionBestSell.sellCurrencyId]!!.id).isEqualTo(shortBestSell.id)

        assertThat(ownershipOriginOrders.bestSellOrder!!.id).isEqualTo(shortOriginBestSell.id)

        assertThat(reconciled.bestSellOrder!!.id).isEqualTo(unionBestSell.id)
        assertThat(reconciled.bestBidOrder!!.id).isEqualTo(unionBestBid.id)
        assertThat(reconciled.originOrders).hasSize(1)

        coVerify {
            testItemEventProducer.send(match<KafkaMessage<ItemEventDto>> { message ->
                message.value is ItemUpdateEventDto && message.value.itemId == itemId
            })
        }
        coVerify(exactly = 1) {
            testOwnershipEventProducer.send(match<KafkaMessage<OwnershipEventDto>> { message ->
                val ownership = (message.value as OwnershipUpdateEventDto).ownership
                ownership.id == ethAuctionedOwnershipId && ownership.bestSellOrder!!.id == unionBestSell.id
            })
        }
        coVerify(exactly = 1) {
            testOwnershipEventProducer.send(match<KafkaMessage<OwnershipEventDto>> { message ->
                val ownership = (message.value as OwnershipUpdateEventDto).ownership
                ownership.id == ethAuctionedOwnershipId && ownership.auction == auction
            })
        }
    }

    @Test
    fun `reconcile ownership - partially auctioned`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()

        val ethAuction = randomEthAuctionDto(ethItemId)
        val auction = ethAuctionConverter.convert(ethAuction, BlockchainDto.ETHEREUM)

        val ethOwnershipId = ethItemId.toOwnership(auction.seller.value)
        val ethOwnership = randomEthOwnershipDto(ethOwnershipId)
        val unionOwnership = EthOwnershipConverter.convert(ethOwnership, ethOwnershipId.blockchain)
        val shortOwnership = ShortOwnershipConverter.convert(unionOwnership)

        val ethBestSell = randomEthSellOrderDto(ethItemId)
        val unionBestSell = ethOrderConverter.convert(ethBestSell, ethOwnershipId.blockchain)
        val shortBestSell = ShortOrderConverter.convert(unionBestSell)

        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(
            ethItemId,
            ethOwnershipId.owner.value,
            listOf(ethAuction)
        )
        ethereumOwnershipControllerApiMock.mockGetNftOwnershipById(ethOwnershipId, ethOwnership)
        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(ethItemId, ethBestSell.take.assetType)
        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(
            ethOwnershipId,
            unionBestSell.sellCurrencyId,
            ethBestSell
        )

        val mintActivity = randomEthItemMintActivity().copy(owner = Address.apply(ethOwnershipId.owner.value))
        ethereumActivityControllerApiMock.mockGetNftActivitiesByItem(
            ethItemId,
            listOf(NftActivityFilterByItemDto.Types.MINT),
            1,
            null,
            ActivitySortDto.LATEST_FIRST,
            mintActivity
        )

        val uri = "$baseUri/v0.1/refresh/ownership/${ethOwnershipId.fullId()}/reconcile"
        val result = testRestTemplate.postForEntity(uri, null, OwnershipEventDto::class.java).body!!
        val reconciled = (result as OwnershipUpdateEventDto).ownership
        val savedShortOwnership = enrichmentOwnershipService.get(shortOwnership.id)!!

        assertThat(savedShortOwnership.source).isEqualTo(OwnershipSourceDto.MINT)
        assertThat(savedShortOwnership.bestSellOrder!!.id).isEqualTo(shortBestSell.id)
        assertThat(savedShortOwnership.bestSellOrders[unionBestSell.sellCurrencyId]!!.id).isEqualTo(shortBestSell.id)

        assertThat(reconciled.bestSellOrder!!.id).isEqualTo(unionBestSell.id)
        assertThat(reconciled.auction).isEqualTo(auction)

        coVerify(exactly = 1) {
            testOwnershipEventProducer.send(match<KafkaMessage<OwnershipEventDto>> { message ->
                message.value is OwnershipUpdateEventDto && message.value.ownershipId == ethOwnershipId
            })
        }
    }

    @Test
    fun `reconcile ownership - fully auctioned`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()

        val ethAuction = randomEthAuctionDto(ethItemId)
        val auction = ethAuctionConverter.convert(ethAuction, BlockchainDto.ETHEREUM)

        val ethOwnershipId = ethItemId.toOwnership(auction.seller.value)
        val auctionOwnershipId = ethItemId.toOwnership(auction.contract.value)
        val auctionOwnership = randomEthOwnershipDto(auctionOwnershipId)

        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(
            ethItemId,
            ethOwnershipId.owner.value,
            listOf(ethAuction)
        )
        ethereumOwnershipControllerApiMock.mockGetNftOwnershipByIdNotFound(ethOwnershipId)
        ethereumOwnershipControllerApiMock.mockGetNftOwnershipById(auctionOwnershipId, auctionOwnership)
        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(ethItemId)

        val uri = "$baseUri/v0.1/refresh/ownership/${ethOwnershipId.fullId()}/reconcile"
        val result = testRestTemplate.postForEntity(uri, null, OwnershipEventDto::class.java).body!!
        val reconciled = (result as OwnershipUpdateEventDto).ownership

        // Nothing to save - there should not be enrichment data for fully-auctioned ownerships
        assertThat(enrichmentOwnershipService.get(ShortOwnershipId(auctionOwnershipId))).isNull()
        assertThat(enrichmentOwnershipService.get(ShortOwnershipId(ethOwnershipId))).isNull()

        assertThat(reconciled.auction).isEqualTo(auction)

        coVerify(exactly = 1) {
            testOwnershipEventProducer.send(match<KafkaMessage<OwnershipEventDto>> { message ->
                message.value is OwnershipUpdateEventDto && message.value.ownershipId == ethOwnershipId
            })
        }
    }

    @Test
    fun `reconcile deleted item`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(ethItemId).copy(deleted = true)

        val ethBestSell = randomEthSellOrderDto(ethItemId)
        val unionBestSell = ethOrderConverter.convert(ethBestSell, ethItemId.blockchain)

        val ethBestBid = randomEthBidOrderDto(ethItemId)
        val unionBestBid = ethOrderConverter.convert(ethBestBid, ethItemId.blockchain)

        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(ethItemId, emptyList())
        ethereumItemControllerApiMock.mockGetNftItemById(ethItemId, ethItem)
        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(ethItemId, ethBestSell.take.assetType)
        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(
            ethItemId,
            unionBestSell.sellCurrencyId,
            ethBestSell
        )

        ethereumOrderControllerApiMock.mockGetCurrenciesByBidOrdersOfItem(ethItemId, ethBestBid.make.assetType)
        ethereumOrderControllerApiMock.mockGetOrderBidsByItemAndByStatus(
            ethItemId,
            unionBestBid.bidCurrencyId,
            ethBestBid
        )
        mockLastSellActivity(ethItemId, null)

        val uri = "$baseUri/v0.1/refresh/item/${ethItemId.fullId()}/reconcile"
        val result = testRestTemplate.postForEntity(uri, null, ItemEventDto::class.java).body!!
        assertThat(result).isInstanceOf(ItemDeleteEventDto::class.java)

        coVerify {
            testItemEventProducer.send(match<KafkaMessage<ItemEventDto>> { message ->
                message.value is ItemDeleteEventDto && message.value.itemId == ethItemId
            })
        }
    }

    @Test
    fun `reconcile collection`() = runBlocking<Unit> {
        val collectionId = CollectionIdDto(BlockchainDto.ETHEREUM, ethOriginCollection)
        val currencyAsset = randomEthAssetErc20()
        val currencyId = EthConverter.convert(currencyAsset, BlockchainDto.ETHEREUM).type.ext.currencyAddress()

        val fakeItemId = ItemIdDto(BlockchainDto.ETHEREUM, collectionId.value, BigInteger("-1"))

        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(fakeItemId, currencyAsset.assetType)
        ethereumOrderControllerApiMock.mockGetCurrenciesByBidOrdersOfItem(fakeItemId, currencyAsset.assetType)

        val sellOrder = randomEthV2OrderDto().copy(
            make = randomEthCollectionAsset(Address.apply(collectionId.value))
        )
        val unionBestSell = ethOrderConverter.convert(sellOrder, collectionId.blockchain)

        val bidOrder = randomEthV2OrderDto().copy(
            take = randomEthCollectionAsset(Address.apply(collectionId.value)),
            make = currencyAsset
        )
        val unionBestBid = ethOrderConverter.convert(bidOrder, collectionId.blockchain)

        val ethCollectionDto = randomEthCollectionDto(Address.apply(collectionId.value))

        coEvery { testEthereumCollectionApi.getNftCollectionById(collectionId.value) } returns ethCollectionDto.toMono()

        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(fakeItemId, currencyId, sellOrder)
        ethereumOrderControllerApiMock.mockGetOrderBidsByItemAndByStatus(fakeItemId, currencyId, bidOrder)
        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(fakeItemId, currencyId, origin, sellOrder)
        ethereumOrderControllerApiMock.mockGetOrderBidsByItemAndByStatus(fakeItemId, currencyId, origin, bidOrder)

        val uri = "$baseUri/v0.1/refresh/collection/${collectionId.fullId()}/reconcile"
        val result = testRestTemplate.postForEntity(uri, null, CollectionUpdateEventDto::class.java).body!!
        val reconciled = result.collection
        val originOrders = reconciled.originOrders!!.toList()[0]

        assertThat(reconciled.bestBidOrder!!.take.type.ext.isCollection).isTrue
        assertThat(reconciled.bestBidOrder!!.take.type.ext.collectionId).isEqualTo(collectionId)
        assertThat(reconciled.bestSellOrder!!.make.type.ext.isCollection).isTrue
        assertThat(reconciled.bestSellOrder!!.make.type.ext.collectionId).isEqualTo(collectionId)

        assertThat(originOrders.bestSellOrder!!.id).isEqualTo(unionBestSell.id)
        assertThat(originOrders.bestBidOrder!!.id).isEqualTo(unionBestBid.id)

        coVerify {
            testCollectionEventProducer.send(match<KafkaMessage<CollectionEventDto>> { message ->
                message.value is CollectionUpdateEventDto && message.value.collectionId == collectionId
            })
        }
    }

    @Test
    fun `reconcile collection - without best orders`() = runBlocking<Unit> {
        val collectionId = randomEthCollectionId()
        val fakeItemId = ItemIdDto(BlockchainDto.ETHEREUM, collectionId.value, BigInteger("-1"))
        val currencyAsset = randomEthAssetErc20()
        val currencyId = EthConverter.convert(currencyAsset, BlockchainDto.ETHEREUM).type.ext.currencyAddress()

        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(fakeItemId, currencyAsset.assetType)
        ethereumOrderControllerApiMock.mockGetCurrenciesByBidOrdersOfItem(fakeItemId, currencyAsset.assetType)

        val sellOrder = randomEthV2OrderDto()
        val bidOrder = randomEthV2OrderDto()

        val ethCollectionDto = randomEthCollectionDto(Address.apply(collectionId.value))

        coEvery { testEthereumCollectionApi.getNftCollectionById(collectionId.value) } returns ethCollectionDto.toMono()

        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(fakeItemId, currencyId, sellOrder)
        ethereumOrderControllerApiMock.mockGetOrderBidsByItemAndByStatus(fakeItemId, currencyId, bidOrder)

        val uri = "$baseUri/v0.1/refresh/collection/${collectionId.fullId()}/reconcile"
        val result = testRestTemplate.postForEntity(uri, null, CollectionUpdateEventDto::class.java).body!!
        val reconciled = result.collection

        assertThat(reconciled.id).isEqualTo(collectionId)
        assertThat(reconciled.bestBidOrder).isNull()
        assertThat(reconciled.bestSellOrder).isNull()

        coVerify {
            testCollectionEventProducer.send(match<KafkaMessage<CollectionEventDto>> { message ->
                message.value is CollectionUpdateEventDto && message.value.collectionId == collectionId
            })
        }
    }

    // TODO should be moved to EnrichmentOrderServiceTest
    @Test
    fun `should ignore best sell order with filled taker`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(ethItemId)
        val unionItem = EthItemConverter.convert(ethItem, ethItemId.blockchain)
        val shortItem = ShortItemConverter.convert(unionItem)

        val ethBestSell = randomEthSellOrderDto(ethItemId).copy(taker = Address.ONE())
        val unionBestSell = ethOrderConverter.convert(ethBestSell, ethItemId.blockchain)

        val ethBestBid = randomEthBidOrderDto(ethItemId)
        val unionBestBid = ethOrderConverter.convert(ethBestBid, ethItemId.blockchain)

        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(ethItemId, emptyList())
        ethereumItemControllerApiMock.mockGetNftItemById(ethItemId, ethItem)
        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(ethItemId, ethBestSell.take.assetType)
        ethereumOrderControllerApiMock.mockGetSellOrdersByItemAndByStatus(
            ethItemId,
            unionBestSell.sellCurrencyId,
            ethBestSell
        )

        ethereumOrderControllerApiMock.mockGetCurrenciesByBidOrdersOfItem(ethItemId, ethBestBid.make.assetType)
        ethereumOrderControllerApiMock.mockGetOrderBidsByItemAndByStatus(
            ethItemId,
            unionBestBid.bidCurrencyId,
            ethBestBid
        )
        mockLastSellActivity(ethItemId, null)

        val uri = "$baseUri/v0.1/refresh/item/${ethItemId.fullId()}/reconcile"
        val result = testRestTemplate.postForEntity(uri, null, ItemEventDto::class.java).body!!
        val reconciled = (result as ItemUpdateEventDto).item
        assertThat(reconciled.bestSellOrder).isNull()
        assertThat(reconciled.bestBidOrder).isNotNull()

        val savedShortItem = enrichmentItemService.get(shortItem.id)!!
        assertThat(savedShortItem.bestSellOrder).isNull()
        assertThat(savedShortItem.bestSellOrders).isEmpty()

        coVerify {
            testItemEventProducer.send(match<KafkaMessage<ItemEventDto>> { message ->
                message.value is ItemUpdateEventDto && message.value.itemId == ethItemId
            })
        }
    }

    // TODO should be moved to EnrichmentOrderServiceTest
    @Test
    fun `should ignore best sell order with filled taker for the first time`() = runBlocking<Unit> {
        val ethItemId = randomEthItemId()
        val (ethItemContract, ethItemTokenId) = CompositeItemIdParser.split(ethItemId.value)
        val ethItem = randomEthNftItemDto(ethItemId)
        val unionItem = EthItemConverter.convert(ethItem, ethItemId.blockchain)
        val shortItem = ShortItemConverter.convert(unionItem)

        val ethBestSell = randomEthSellOrderDto(ethItemId)
        val ethBestSellWithTaker = randomEthSellOrderDto(ethItemId).copy(taker = Address.ONE())
        val unionBestSell = ethOrderConverter.convert(ethBestSell, ethItemId.blockchain)
        val shortBestSell = ShortOrderConverter.convert(unionBestSell)

        val ethBestBid = randomEthBidOrderDto(ethItemId)
        val unionBestBid = ethOrderConverter.convert(ethBestBid, ethItemId.blockchain)

        ethereumAuctionControllerApiMock.mockGetAuctionsByItem(ethItemId, emptyList())
        ethereumItemControllerApiMock.mockGetNftItemById(ethItemId, ethItem)
        ethereumOrderControllerApiMock.mockGetCurrenciesBySellOrdersOfItem(
            ethItemId,
            ethBestSellWithTaker.take.assetType
        )
        val continuation = "continuation"
        every {
            testEthereumOrderApi.getSellOrdersByItemAndByStatus(
                eq(ethItemContract),
                eq(ethItemTokenId.toString()),
                any(),
                any(),
                any(),
                isNull(),
                any(),
                any(),
                any()
            )
        } returns Mono.just(OrdersPaginationDto(listOf(ethBestSellWithTaker), continuation))

        every {
            testEthereumOrderApi.getSellOrdersByItemAndByStatus(
                eq(ethItemContract),
                eq(ethItemTokenId.toString()),
                any(),
                any(),
                any(),
                eq(continuation),
                any(),
                any(),
                any()
            )
        } returns Mono.just(OrdersPaginationDto(listOf(ethBestSell), continuation))

        ethereumOrderControllerApiMock.mockGetCurrenciesByBidOrdersOfItem(ethItemId, ethBestBid.make.assetType)
        ethereumOrderControllerApiMock.mockGetOrderBidsByItemAndByStatus(
            ethItemId,
            unionBestBid.bidCurrencyId,
            ethBestBid
        )
        mockLastSellActivity(ethItemId, null)

        val uri = "$baseUri/v0.1/refresh/item/${ethItemId.fullId()}/reconcile"
        val result = testRestTemplate.postForEntity(uri, null, ItemEventDto::class.java).body!!
        val reconciled = (result as ItemUpdateEventDto).item

        assertThat(reconciled.bestSellOrder!!.id).isEqualTo(unionBestSell.id)

        val savedShortItem = enrichmentItemService.get(shortItem.id)!!
        assertThat(savedShortItem.bestSellOrder!!.id).isEqualTo(shortBestSell.id)
        assertThat(savedShortItem.bestSellOrders[unionBestSell.sellCurrencyId]!!.id).isEqualTo(shortBestSell.id)


        coVerify {
            testItemEventProducer.send(match<KafkaMessage<ItemEventDto>> { message ->
                message.value is ItemUpdateEventDto && message.value.itemId == ethItemId
            })
        }
    }

    private fun mockLastSellActivity(itemId: ItemIdDto, activity: OrderActivityMatchDto?) {
        ethereumActivityControllerApiMock.mockGetOrderActivitiesByItem(
            itemId,
            listOf(OrderActivityFilterByItemDto.Types.MATCH),
            1,
            null,
            ActivitySortDto.LATEST_FIRST,
            activity
        )
    }
}
