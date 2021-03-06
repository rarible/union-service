package com.rarible.protocol.union.listener.service

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.union.core.model.ReconciliationMarkType
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.AuctionStatusDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.converter.EnrichedItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortItemConverter
import com.rarible.protocol.union.enrichment.converter.ShortOrderConverter
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ReconciliationMarkRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.test.data.randomShortItem
import com.rarible.protocol.union.enrichment.test.data.randomShortOwnership
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.enrichment.test.data.randomUnionSellOrderDto
import com.rarible.protocol.union.enrichment.util.bidCurrencyId
import com.rarible.protocol.union.integration.ethereum.converter.EthAuctionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthMetaConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthBidOrderDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemMeta
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthSellOrderDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.coEvery
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class EnrichmentItemEventServiceIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var itemEventService: EnrichmentItemEventService

    @Autowired
    private lateinit var itemService: EnrichmentItemService

    @Autowired
    private lateinit var ownershipService: EnrichmentOwnershipService

    @Autowired
    lateinit var ethOrderConverter: EthOrderConverter

    @Autowired
    lateinit var ethAuctionConverter: EthAuctionConverter

    @Autowired
    private lateinit var itemReconciliationMarkRepository: ReconciliationMarkRepository

    @Test
    fun `update event - item doesn't exist`() = runWithKafka {
        val itemId = randomEthItemId()
        val unionItem = randomUnionItem(itemId)

        itemEventService.onItemUpdated(unionItem)

        val created = itemService.get(ShortItemId(itemId))

        // Item should not be updated since it wasn't in DB before update
        assertThat(created).isNull()
        // But there should be single Item event "as is"
        waitAssert {
            val messages = findItemUpdates(itemId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(itemId.fullId())
            assertThat(messages[0].id).isEqualTo(itemId.fullId())
            assertThat(messages[0].value.itemId).isEqualTo(itemId)

            // TODO: see CHARLIE-158: here we ensure that meta is taken from the blockchain's Item.
            assertThat(messages[0].value.item).isEqualTo(
                EnrichedItemConverter.convert(unionItem, meta = unionItem.meta)
            )
        }
    }

    @Test
    fun `update event - existing item updated`() = runWithKafka {
        val itemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(itemId)
        val bestSellOrder = randomEthSellOrderDto(itemId)
        val bestBidOrder = randomEthSellOrderDto(itemId)
        val unionBestSell = ethOrderConverter.convert(bestSellOrder, itemId.blockchain)
        val unionBestBid = ethOrderConverter.convert(bestBidOrder, itemId.blockchain)

        val unionItem = EthItemConverter.convert(ethItem, itemId.blockchain)
        val shortItem = ShortItemConverter.convert(unionItem).copy(
            bestSellOrder = ShortOrderConverter.convert(unionBestSell),
            bestBidOrder = ShortOrderConverter.convert(unionBestBid)
        )

        itemService.save(shortItem)

        ethereumOrderControllerApiMock.mockGetByIds(bestSellOrder, bestBidOrder)
        ethereumItemControllerApiMock.mockGetNftItemById(itemId, ethItem)

        itemEventService.onItemUpdated(unionItem)

        val expected = EnrichedItemConverter.convert(unionItem)
            .copy(
                bestSellOrder = unionBestSell,
                bestBidOrder = unionBestBid
            )

        val saved = itemService.get(shortItem.id)!!
        assertThat(saved.bestSellOrder).isEqualTo(shortItem.bestSellOrder)
        assertThat(saved.bestBidOrder).isEqualTo(shortItem.bestBidOrder)

        waitAssert {
            val messages = findItemUpdates(itemId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(itemId.fullId())
            assertThat(messages[0].id).isEqualTo(itemId.fullId())
            assertThat(messages[0].value.itemId).isEqualTo(itemId)
            assertThat(messages[0].value.item.id).isEqualTo(expected.id)
            assertThat(messages[0].value.item.bestSellOrder!!.id).isEqualTo(expected.bestSellOrder!!.id)
            assertThat(messages[0].value.item.bestBidOrder!!.id).isEqualTo(expected.bestBidOrder!!.id)
        }
    }

    @Test
    fun `update event - existing item updated, order corrupted`() = runWithKafka {
        val itemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(itemId)

        // Corrupted order with taker
        val bestBidOrder = randomEthSellOrderDto(itemId).copy(taker = randomAddress())
        val unionBestBid = ethOrderConverter.convert(bestBidOrder, itemId.blockchain)

        val unionItem = EthItemConverter.convert(ethItem, itemId.blockchain)
        val shortItem = ShortItemConverter.convert(unionItem).copy(
            bestBidOrder = ShortOrderConverter.convert(unionBestBid)
        )

        itemService.save(shortItem)

        ethereumOrderControllerApiMock.mockGetByIds(bestBidOrder)
        ethereumItemControllerApiMock.mockGetNftItemById(itemId, ethItem)

        itemEventService.onItemUpdated(unionItem)

        waitAssert {
            // Event should not be sent in case of corrupted enrichment data
            val messages = findItemUpdates(itemId.value)
            assertThat(messages).hasSize(0)

            // Reconciliation mark should be created for such item
            val reconcileMarks = itemReconciliationMarkRepository.findByType(ReconciliationMarkType.ITEM, 100)
            val expectedMark = reconcileMarks.find { it.id == itemId.fullId() }
            assertThat(expectedMark).isNotNull()
        }
    }

    @Test
    fun `on ownership updated`() = runWithKafka {
        val itemId = randomEthItemId()
        val shortItem = randomShortItem(itemId).copy(sellers = 3, totalStock = 20.toBigInteger())
        val ethItem = randomEthNftItemDto(itemId)
        val ethMeta = randomEthItemMeta()
        val unionMeta = EthMetaConverter.convert(ethMeta)
        val unionItem = EthItemConverter.convert(ethItem, itemId.blockchain)
        itemService.save(shortItem)

        coEvery { testItemMetaLoader.load(itemId) } returns unionMeta

        val bestSellOrder1 = randomUnionSellOrderDto(itemId).copy(makeStock = 20.toBigDecimal())
        val ownership1 = randomShortOwnership(itemId).copy(bestSellOrder = ShortOrderConverter.convert(bestSellOrder1))
        ownershipService.save(ownership1)

        val bestSellOrder2 = randomUnionSellOrderDto(itemId).copy(makeStock = 10.toBigDecimal())
        val ownership2 = randomShortOwnership(itemId).copy(bestSellOrder = ShortOrderConverter.convert(bestSellOrder2))
        ownershipService.save(ownership2)

        ethereumItemControllerApiMock.mockGetNftItemById(itemId, ethItem)
        ethereumItemControllerApiMock.mockGetNftItemMetaById(itemId, ethMeta)

        itemEventService.onOwnershipUpdated(ownership1.id, bestSellOrder1)

        val saved = itemService.get(shortItem.id)!!
        assertThat(saved.sellers).isEqualTo(2)
        assertThat(saved.totalStock).isEqualTo(30.toBigInteger())

        // In result event for item we expect updated totalStock/sellers
        val expected = EnrichedItemConverter.convert(unionItem, meta = unionMeta).copy(
            sellers = 2,
            totalStock = 30.toBigInteger()
        )

        waitAssert {
            val messages = findItemUpdates(itemId.value)
            // There may be several events for item update (when meta gets loaded)
            // TODO but since we're testing it on service level, first message
            // sent by meta-loader with not-updated Item enrichment data
            assertThat(messages).hasSize(2)
            assertThat(messages[1].value.itemId).isEqualTo(itemId)
            assertThat(messages[1].value.item).isEqualTo(expected)
        }
    }

    @Test
    fun `on ownership updated - sell stats not changed`() = runWithKafka<Unit> {
        val itemId = randomEthItemId()
        val shortItem = randomShortItem(itemId).copy(sellers = 1, totalStock = 20.toBigInteger())
        // Item should not be changed - we'll check version
        val expectedItem = itemService.save(shortItem)

        val bestSellOrder = randomUnionSellOrderDto(itemId).copy(makeStock = 20.toBigDecimal())
        val ownership = randomShortOwnership(itemId).copy(bestSellOrder = ShortOrderConverter.convert(bestSellOrder))
        ownershipService.save(ownership)

        itemEventService.onOwnershipUpdated(ownership.id, bestSellOrder)

        val saved = itemService.get(expectedItem.id)!!

        assertThat(saved.version).isEqualTo(expectedItem.version)
    }

    @Test
    fun `on best sell order updated - item exists`() = runWithKafka {
        val itemId = randomEthItemId()
        val shortItem = randomShortItem(itemId)
        val ethItem = randomEthNftItemDto(itemId)
        val ethMeta = randomEthItemMeta()
        val unionItem = EthItemConverter.convert(ethItem, itemId.blockchain)
        itemService.save(shortItem)

        val bestSellOrder = randomEthSellOrderDto(itemId)
        val unionBestSell = ethOrderConverter.convert(bestSellOrder, itemId.blockchain)

        ethereumItemControllerApiMock.mockGetNftItemById(itemId, ethItem)
        ethereumItemControllerApiMock.mockGetNftItemMetaById(itemId, ethMeta)

        itemEventService.onItemBestSellOrderUpdated(shortItem.id, unionBestSell)

        // In result event for Item we expect updated bestSellOrder
        val expected = EnrichedItemConverter.convert(unionItem).copy(bestSellOrder = unionBestSell)

        val saved = itemService.get(shortItem.id)!!
        assertThat(saved.bestSellOrder).isEqualTo(ShortOrderConverter.convert(unionBestSell))

        waitAssert {
            val messages = findItemUpdates(itemId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].value.itemId).isEqualTo(itemId)
            assertThat(messages[0].value.item.id).isEqualTo(expected.id)
            assertThat(messages[0].value.item.bestSellOrder!!.id).isEqualTo(expected.bestSellOrder!!.id)
            assertThat(messages[0].value.item.bestBidOrder).isNull()
        }
    }

    @Test
    fun `on best bid order updated - item exists with same order, order cancelled`() = runWithKafka {
        val itemId = randomEthItemId()
        val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)

        val bestBidOrder = randomEthBidOrderDto(itemId).copy(status = OrderStatusDto.CANCELLED)
        val unionBestBid = ethOrderConverter.convert(bestBidOrder, itemId.blockchain)

        val shortItem = randomShortItem(itemId).copy(bestBidOrder = ShortOrderConverter.convert(unionBestBid))
        val ethItem = randomEthNftItemDto(itemId)
        val ethMeta = randomEthItemMeta()
        itemService.save(shortItem)

        ethereumItemControllerApiMock.mockGetNftItemById(itemId, ethItem)
        ethereumItemControllerApiMock.mockGetNftItemMetaById(itemId, ethMeta)
        ethereumOrderControllerApiMock.mockGetOrderBidsByItemAndByStatus(itemId, unionBestBid.bidCurrencyId)

        itemEventService.onItemBestBidOrderUpdated(shortItem.id, unionBestBid)

        // Item should be removed since it has no enrich data
        val saved = itemService.get(shortItem.id)
        assertThat(saved).isNull()

        waitAssert {
            val messages = findItemUpdates(itemId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].value.itemId).isEqualTo(itemId)
            assertThat(messages[0].value.item.bestBidOrder).isNull()
        }
    }

    @Test
    fun `on best bid order updated - item doesn't exists, order cancelled`() = runWithKafka {
        val itemId = randomEthItemId()
        val shortItem = randomShortItem(itemId)
        val ethItem = randomEthNftItemDto(itemId)
        // In this case we don't have saved ShortItem in Enrichment DB

        val bestBidOrder = randomEthBidOrderDto(itemId).copy(status = OrderStatusDto.INACTIVE)
        val unionBestBid = ethOrderConverter.convert(bestBidOrder, itemId.blockchain)

        ethereumItemControllerApiMock.mockGetNftItemById(itemId, ethItem)

        itemEventService.onItemBestBidOrderUpdated(shortItem.id, unionBestBid)

        val saved = itemService.get(shortItem.id)
        assertThat(saved).isNull()

        // Unfortunately, there is no other way to ensure there is no messages in the Kafka
        delay(1000)
        waitAssert {
            assertThat(itemEvents).hasSize(0)
        }
    }

    @Test
    fun `delete event - existing item deleted`() = runWithKafka {
        val item = itemService.save(randomShortItem())
        val itemId = item.id.toDto()
        assertThat(itemService.get(item.id)).isNotNull()

        itemEventService.onItemDeleted(itemId)

        assertThat(itemService.get(item.id)).isNull()
        waitAssert {
            val messages = findItemDeletions(itemId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(itemId.fullId())
            assertThat(messages[0].id).isEqualTo(itemId.fullId())
            assertThat(messages[0].value.itemId).isEqualTo(itemId)
        }
    }

    @Test
    fun `delete event - item doesn't exist`() = runWithKafka {
        val shortItemId = randomShortItem().id
        val itemId = shortItemId.toDto()

        itemEventService.onItemDeleted(itemId)

        assertThat(itemService.get(shortItemId)).isNull()
        waitAssert {
            val messages = findItemDeletions(itemId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].key).isEqualTo(itemId.fullId())
            assertThat(messages[0].id).isEqualTo(itemId.fullId())
            assertThat(messages[0].value.itemId).isEqualTo(itemId)
        }
    }

    @Test
    fun `damn dot`() = runBlocking<Unit> {
        val item = ShortItemConverter.convert(randomUnionItem(randomEthItemId()))

        val itemWithDotMapKey = item.copy(
            bestSellOrders = mapOf("A.something.Flow" to ShortOrderConverter.convert(randomUnionSellOrderDto()))
        )

        val saved = itemService.save(itemWithDotMapKey)
        val fromMongo = itemService.get(item.id)!!

        assertThat(itemWithDotMapKey).isEqualTo(saved.copy(version = null, lastUpdatedAt = item.lastUpdatedAt))
        assertThat(itemWithDotMapKey).isEqualTo(fromMongo.copy(version = null, lastUpdatedAt = item.lastUpdatedAt))
    }

    @Test
    fun `on auction update`() = runWithKafka {
        val itemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(itemId)
        val ethMeta = randomEthItemMeta()

        val unionItem = EthItemConverter.convert(ethItem, itemId.blockchain)
        val shortItem = ShortItemConverter.convert(unionItem)
        val auction = ethAuctionConverter.convert(randomEthAuctionDto(itemId), BlockchainDto.ETHEREUM)
        itemService.save(shortItem)

        ethereumItemControllerApiMock.mockGetNftItemById(itemId, ethItem)
        ethereumItemControllerApiMock.mockGetNftItemMetaById(itemId, ethMeta)

        itemEventService.onAuctionUpdated(auction)

        val saved = itemService.get(shortItem.id)!!
        assertThat(saved.auctions).isEqualTo(setOf(auction.id))

        waitAssert {
            val messages = findItemUpdates(itemId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].value.itemId).isEqualTo(itemId)
            assertThat(messages[0].value.item.auctions.size).isEqualTo(1)
        }
    }

    @Test
    fun `on auction update - inactive removed`() = runWithKafka {
        val itemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(itemId)
        val ethMeta = randomEthItemMeta()

        val auction = ethAuctionConverter.convert(randomEthAuctionDto(itemId), BlockchainDto.ETHEREUM)
            .copy(status = AuctionStatusDto.CANCELLED)

        val unionItem = EthItemConverter.convert(ethItem, itemId.blockchain)
        val shortItem = ShortItemConverter.convert(unionItem)
            .copy(auctions = setOf(auction.id))

        itemService.save(shortItem)

        ethereumItemControllerApiMock.mockGetNftItemById(itemId, ethItem)
        ethereumItemControllerApiMock.mockGetNftItemMetaById(itemId, ethMeta)

        itemEventService.onAuctionUpdated(auction)

        val saved = itemService.get(shortItem.id)
        // No enrich data, should be removed
        assertThat(saved).isNull()

        waitAssert {
            val messages = findItemUpdates(itemId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].value.itemId).isEqualTo(itemId)
            assertThat(messages[0].value.item.auctions.size).isEqualTo(0)
        }
    }

    @Test
    fun `on auction update - another auction fetched`() = runWithKafka {
        val itemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(itemId)
        val ethMeta = randomEthItemMeta()

        val unionItem = EthItemConverter.convert(ethItem, itemId.blockchain)
        val shortItem = ShortItemConverter.convert(unionItem)
        val ethAuction = randomEthAuctionDto(itemId)
        val auction = ethAuctionConverter.convert(ethAuction, BlockchainDto.ETHEREUM)
        itemService.save(shortItem)

        ethereumItemControllerApiMock.mockGetNftItemById(itemId, ethItem)
        ethereumItemControllerApiMock.mockGetNftItemMetaById(itemId, ethMeta)
        ethereumAuctionControllerApiMock.mockGetAuctionsByIds(ethAuction)

        itemEventService.onAuctionUpdated(auction)

        val newAuction = ethAuctionConverter.convert(randomEthAuctionDto(itemId), BlockchainDto.ETHEREUM)
        itemEventService.onAuctionUpdated(newAuction)

        val saved = itemService.get(shortItem.id)!!
        assertThat(saved.auctions.size).isEqualTo(2)

        waitAssert {
            val messages = findItemUpdates(itemId.value)
            assertThat(messages).hasSize(2)

            messages.filter { it.value.item.auctions.size == 2 }.map { it.value }.forEach {
                assertThat(messages[0].value.itemId).isEqualTo(itemId)
                assertThat(it.item.auctions.size).isEqualTo(2)
            }
        }
    }

    @Test
    fun `on auction delete`() = runWithKafka {
        val itemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(itemId)
        val ethMeta = randomEthItemMeta()

        val bestSell = randomEthSellOrderDto()
        val unionBestSell = ethOrderConverter.convert(bestSell, BlockchainDto.ETHEREUM)

        val ethAuction = randomEthAuctionDto(itemId)
        val auction = ethAuctionConverter.convert(ethAuction, BlockchainDto.ETHEREUM)

        val unionItem = EthItemConverter.convert(ethItem, itemId.blockchain)
        val shortItem = ShortItemConverter.convert(unionItem).copy(
            auctions = setOf(auction.id),
            bestSellOrder = ShortOrderConverter.convert(unionBestSell)
        )
        itemService.save(shortItem)

        ethereumItemControllerApiMock.mockGetNftItemById(itemId, ethItem)
        ethereumItemControllerApiMock.mockGetNftItemMetaById(itemId, ethMeta)
        ethereumOrderControllerApiMock.mockGetByIds(bestSell)

        itemEventService.onAuctionDeleted(auction)

        val saved = itemService.get(shortItem.id)!!
        // Should be not deleted since there is some enrich data
        assertThat(saved.auctions).isNullOrEmpty()

        waitAssert {
            val messages = findItemUpdates(itemId.value)
            assertThat(messages).hasSize(1)
            assertThat(messages[0].value.itemId).isEqualTo(itemId)
            assertThat(messages[0].value.item.auctions).isEmpty()
        }
    }
}
