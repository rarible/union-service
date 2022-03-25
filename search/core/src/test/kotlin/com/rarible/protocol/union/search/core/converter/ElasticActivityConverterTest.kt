package com.rarible.protocol.union.search.core.converter

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBoolean
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.dto.ActivityBlockchainInfoDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.AuctionBidActivityDto
import com.rarible.protocol.union.dto.AuctionBidDto
import com.rarible.protocol.union.dto.AuctionCancelActivityDto
import com.rarible.protocol.union.dto.AuctionDataDto
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionEndActivityDto
import com.rarible.protocol.union.dto.AuctionFinishActivityDto
import com.rarible.protocol.union.dto.AuctionHistoryDto
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.AuctionOpenActivityDto
import com.rarible.protocol.union.dto.AuctionStartActivityDto
import com.rarible.protocol.union.dto.AuctionStatusDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.EthErc1155AssetTypeDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderActivityMatchSideDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import com.rarible.protocol.union.dto.OrderBidActivityDto
import com.rarible.protocol.union.dto.OrderCancelBidActivityDto
import com.rarible.protocol.union.dto.OrderCancelListActivityDto
import com.rarible.protocol.union.dto.OrderListActivityDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.OrderMatchSwapDto
import com.rarible.protocol.union.dto.RaribleAuctionV1BidDataV1Dto
import com.rarible.protocol.union.dto.RaribleAuctionV1BidV1Dto
import com.rarible.protocol.union.dto.RaribleAuctionV1DataV1Dto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.ext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class ElasticActivityConverterTest {

    private val converter = ElasticActivityConverter()

    @Test
    fun `should convert MintActivityDto`() {
        // given
        val source = MintActivityDto(
            id = randomActivityId(),
            date = randomDate(),
            blockchainInfo = ActivityBlockchainInfoDto(
                transactionHash = randomString(),
                blockHash = randomString(),
                blockNumber = randomLong(),
                logIndex = randomInt(),
            ),
            owner = randomUnionAddress(),
            itemId = randomItemId(),
            transactionHash = randomString(),
            value = randomBigInt(),
        )

        // when
        val actual = converter.convert(source)

        // then
        assertThat(actual.activityId).isEqualTo(source.id.value)
        assertThat(actual.date).isEqualTo(source.date)
        assertThat(actual.blockNumber).isEqualTo(source.blockchainInfo!!.blockNumber)
        assertThat(actual.logIndex).isEqualTo(source.blockchainInfo!!.logIndex)
        assertThat(actual.blockchain).isEqualTo(source.id.blockchain)
        assertThat(actual.type).isEqualTo(ActivityTypeDto.MINT)
        assertThat(actual.user.maker).isEqualTo(source.owner.value)
        assertThat(actual.user.taker).isNull()
        assertThat(actual.collection.make).isEqualTo(source.itemId!!.value.split(":").first())
        assertThat(actual.collection.take).isNull()
        assertThat(actual.item.make).isEqualTo(source.itemId!!.value)
        assertThat(actual.item.take).isNull()
    }

    @Test
    fun `should convert BurnActivityDto`() {
        // given
        val source = BurnActivityDto(
            id = randomActivityId(),
            date = randomDate(),
            blockchainInfo = ActivityBlockchainInfoDto(
                transactionHash = randomString(),
                blockHash = randomString(),
                blockNumber = randomLong(),
                logIndex = randomInt(),
            ),
            owner = randomUnionAddress(),
            contract = ContractAddress(randomBlockchain(), randomString()),
            tokenId = randomBigInt(),
            transactionHash = randomString(),
            value = randomBigInt(),
        )

        // when
        val actual = converter.convert(source)

        // then
        assertThat(actual.activityId).isEqualTo(source.id.value)
        assertThat(actual.date).isEqualTo(source.date)
        assertThat(actual.blockNumber).isEqualTo(source.blockchainInfo!!.blockNumber)
        assertThat(actual.logIndex).isEqualTo(source.blockchainInfo!!.logIndex)
        assertThat(actual.blockchain).isEqualTo(source.id.blockchain)
        assertThat(actual.type).isEqualTo(ActivityTypeDto.BURN)
        assertThat(actual.user.maker).isEqualTo(source.owner.value)
        assertThat(actual.user.taker).isNull()
        assertThat(actual.collection.make).isEqualTo(source.contract!!.value)
        assertThat(actual.collection.take).isNull()
        assertThat(actual.item.make).isEqualTo("${source.contract!!.value}:${source.tokenId}")
        assertThat(actual.item.take).isNull()
    }

    @Test
    fun `should convert TransferActivityDto`() {
        // given
        val source = TransferActivityDto(
            id = randomActivityId(),
            date = randomDate(),
            blockchainInfo = ActivityBlockchainInfoDto(
                transactionHash = randomString(),
                blockHash = randomString(),
                blockNumber = randomLong(),
                logIndex = randomInt(),
            ),
            from = randomUnionAddress(),
            owner = randomUnionAddress(),
            contract = ContractAddress(randomBlockchain(), randomString()),
            tokenId = randomBigInt(),
            transactionHash = randomString(),
            value = randomBigInt(),
        )

        // when
        val actual = converter.convert(source)

        // then
        assertThat(actual.activityId).isEqualTo(source.id.value)
        assertThat(actual.date).isEqualTo(source.date)
        assertThat(actual.blockNumber).isEqualTo(source.blockchainInfo!!.blockNumber)
        assertThat(actual.logIndex).isEqualTo(source.blockchainInfo!!.logIndex)
        assertThat(actual.blockchain).isEqualTo(source.id.blockchain)
        assertThat(actual.type).isEqualTo(ActivityTypeDto.TRANSFER)
        assertThat(actual.user.maker).isEqualTo(source.from.value)
        assertThat(actual.user.taker).isEqualTo(source.owner.value)
        assertThat(actual.collection.make).isEqualTo(source.contract!!.value)
        assertThat(actual.collection.take).isNull()
        assertThat(actual.item.make).isEqualTo("${source.contract!!.value}:${source.tokenId}")
        assertThat(actual.item.take).isNull()
    }

    @Test
    fun `should convert OrderMatchSwapDto`() {
        // given
        val source = OrderMatchSwapDto(
            id = randomActivityId(),
            date = randomDate(),
            blockchainInfo = ActivityBlockchainInfoDto(
                transactionHash = randomString(),
                blockHash = randomString(),
                blockNumber = randomLong(),
                logIndex = randomInt(),
            ),
            transactionHash = randomString(),
            left = randomOrderMatchSide(),
            right = randomOrderMatchSide(),
            source = OrderActivitySourceDto.RARIBLE,
        )

        // when
        val actual = converter.convert(source)

        // then
        assertThat(actual.activityId).isEqualTo(source.id.value)
        assertThat(actual.date).isEqualTo(source.date)
        assertThat(actual.blockNumber).isEqualTo(source.blockchainInfo!!.blockNumber)
        assertThat(actual.logIndex).isEqualTo(source.blockchainInfo!!.logIndex)
        assertThat(actual.blockchain).isEqualTo(source.id.blockchain)
        assertThat(actual.type).isEqualTo(ActivityTypeDto.SELL)
        assertThat(actual.user.maker).isEqualTo(source.left.maker.value)
        assertThat(actual.user.taker).isEqualTo(source.right.maker.value)
        assertThat(actual.collection.make).isEqualTo(source.left.asset.type.ext.contract)
        assertThat(actual.collection.take).isEqualTo(source.right.asset.type.ext.contract)
        assertThat(actual.item.make).isEqualTo(source.left.asset.type.ext.itemId!!.value)
        assertThat(actual.item.take).isEqualTo(source.right.asset.type.ext.itemId!!.value)
    }

    @Test
    fun `should convert OrderMatchSellDto`() {
        // given
        val source = OrderMatchSellDto(
            id = randomActivityId(),
            date = randomDate(),
            blockchainInfo = ActivityBlockchainInfoDto(
                transactionHash = randomString(),
                blockHash = randomString(),
                blockNumber = randomLong(),
                logIndex = randomInt(),
            ),
            transactionHash = randomString(),
            buyer = randomUnionAddress(),
            seller = randomUnionAddress(),
            nft = randomAsset(),
            payment = randomAsset(),
            source = OrderActivitySourceDto.RARIBLE,
            price = randomBigDecimal(),
            type = OrderMatchSellDto.Type.SELL,

            )

        // when
        val actual = converter.convert(source)

        // then
        assertThat(actual.activityId).isEqualTo(source.id.value)
        assertThat(actual.date).isEqualTo(source.date)
        assertThat(actual.blockNumber).isEqualTo(source.blockchainInfo!!.blockNumber)
        assertThat(actual.logIndex).isEqualTo(source.blockchainInfo!!.logIndex)
        assertThat(actual.blockchain).isEqualTo(source.id.blockchain)
        assertThat(actual.type).isEqualTo(ActivityTypeDto.SELL)
        assertThat(actual.user.maker).isEqualTo(source.seller.value)
        assertThat(actual.user.taker).isEqualTo(source.buyer.value)
        assertThat(actual.collection.make).isEqualTo(source.nft.type.ext.contract)
        assertThat(actual.collection.take).isEqualTo(source.payment.type.ext.contract)
        assertThat(actual.item.make).isEqualTo(source.nft.type.ext.itemId!!.value)
        assertThat(actual.item.take).isEqualTo(source.payment.type.ext.itemId!!.value)
    }

    @Test
    fun `should convert OrderBidActivityDto`() {
        // given
        val source = OrderBidActivityDto(
            id = randomActivityId(),
            date = randomDate(),
            maker = randomUnionAddress(),
            make = randomAsset(),
            take = randomAsset(),
            source = OrderActivitySourceDto.RARIBLE,
            price = randomBigDecimal(),
            hash = randomString(),
        )

        // when
        val actual = converter.convert(source)

        // then
        assertThat(actual.activityId).isEqualTo(source.id.value)
        assertThat(actual.date).isEqualTo(source.date)
        assertThat(actual.blockNumber).isNull()
        assertThat(actual.logIndex).isNull()
        assertThat(actual.blockchain).isEqualTo(source.id.blockchain)
        assertThat(actual.type).isEqualTo(ActivityTypeDto.BID)
        assertThat(actual.user.maker).isEqualTo(source.maker.value)
        assertThat(actual.user.taker).isNull()
        assertThat(actual.collection.make).isEqualTo(source.make.type.ext.contract)
        assertThat(actual.collection.take).isEqualTo(source.take.type.ext.contract)
        assertThat(actual.item.make).isEqualTo(source.make.type.ext.itemId!!.value)
        assertThat(actual.item.take).isEqualTo(source.take.type.ext.itemId!!.value)
    }

    @Test
    fun `should convert OrderListActivityDto`() {
        // given
        val source = OrderListActivityDto(
            id = randomActivityId(),
            date = randomDate(),
            maker = randomUnionAddress(),
            make = randomAsset(),
            take = randomAsset(),
            source = OrderActivitySourceDto.RARIBLE,
            price = randomBigDecimal(),
            hash = randomString(),
        )

        // when
        val actual = converter.convert(source)

        // then
        assertThat(actual.activityId).isEqualTo(source.id.value)
        assertThat(actual.date).isEqualTo(source.date)
        assertThat(actual.blockNumber).isNull()
        assertThat(actual.logIndex).isNull()
        assertThat(actual.blockchain).isEqualTo(source.id.blockchain)
        assertThat(actual.type).isEqualTo(ActivityTypeDto.LIST)
        assertThat(actual.user.maker).isEqualTo(source.maker.value)
        assertThat(actual.user.taker).isNull()
        assertThat(actual.collection.make).isEqualTo(source.make.type.ext.contract)
        assertThat(actual.collection.take).isEqualTo(source.take.type.ext.contract)
        assertThat(actual.item.make).isEqualTo(source.make.type.ext.itemId!!.value)
        assertThat(actual.item.take).isEqualTo(source.take.type.ext.itemId!!.value)
    }

    @Test
    fun `should convert OrderCancelBidActivityDto`() {
        // given
        val source = OrderCancelBidActivityDto(
            id = randomActivityId(),
            date = randomDate(),
            maker = randomUnionAddress(),
            make = randomAssetType(),
            take = randomAssetType(),
            source = OrderActivitySourceDto.RARIBLE,
            hash = randomString(),
            transactionHash = randomString(),
            blockchainInfo = ActivityBlockchainInfoDto(
                transactionHash = randomString(),
                blockHash = randomString(),
                blockNumber = randomLong(),
                logIndex = randomInt(),
            ),
        )

        // when
        val actual = converter.convert(source)

        // then
        assertThat(actual.activityId).isEqualTo(source.id.value)
        assertThat(actual.date).isEqualTo(source.date)
        assertThat(actual.blockNumber).isEqualTo(source.blockchainInfo!!.blockNumber)
        assertThat(actual.logIndex).isEqualTo(source.blockchainInfo!!.logIndex)
        assertThat(actual.blockchain).isEqualTo(source.id.blockchain)
        assertThat(actual.type).isEqualTo(ActivityTypeDto.CANCEL_BID)
        assertThat(actual.user.maker).isEqualTo(source.maker.value)
        assertThat(actual.user.taker).isNull()
        assertThat(actual.collection.make).isEqualTo(source.make.ext.contract)
        assertThat(actual.collection.take).isEqualTo(source.take.ext.contract)
        assertThat(actual.item.make).isEqualTo(source.make.ext.itemId!!.value)
        assertThat(actual.item.take).isEqualTo(source.take.ext.itemId!!.value)
    }

    @Test
    fun `should convert OrderCancelListActivityDto`() {
        // given
        val source = OrderCancelListActivityDto(
            id = randomActivityId(),
            date = randomDate(),
            maker = randomUnionAddress(),
            make = randomAssetType(),
            take = randomAssetType(),
            source = OrderActivitySourceDto.RARIBLE,
            hash = randomString(),
            transactionHash = randomString(),
            blockchainInfo = ActivityBlockchainInfoDto(
                transactionHash = randomString(),
                blockHash = randomString(),
                blockNumber = randomLong(),
                logIndex = randomInt(),
            ),
        )

        // when
        val actual = converter.convert(source)

        // then
        assertThat(actual.activityId).isEqualTo(source.id.value)
        assertThat(actual.date).isEqualTo(source.date)
        assertThat(actual.blockNumber).isEqualTo(source.blockchainInfo!!.blockNumber)
        assertThat(actual.logIndex).isEqualTo(source.blockchainInfo!!.logIndex)
        assertThat(actual.blockchain).isEqualTo(source.id.blockchain)
        assertThat(actual.type).isEqualTo(ActivityTypeDto.CANCEL_LIST)
        assertThat(actual.user.maker).isEqualTo(source.maker.value)
        assertThat(actual.user.taker).isNull()
        assertThat(actual.collection.make).isEqualTo(source.make.ext.contract)
        assertThat(actual.collection.take).isEqualTo(source.take.ext.contract)
        assertThat(actual.item.make).isEqualTo(source.make.ext.itemId!!.value)
        assertThat(actual.item.take).isEqualTo(source.take.ext.itemId!!.value)
    }

    @Test
    fun `should convert AuctionOpenActivityDto`() {
        // given
        val source = AuctionOpenActivityDto(
            id = randomActivityId(),
            date = randomDate(),
            auction = randomAuction(),
            transactionHash = randomString(),
        )

        // when
        val actual = converter.convert(source)

        // then
        assertThat(actual.activityId).isEqualTo(source.id.value)
        assertThat(actual.date).isEqualTo(source.date)
        assertThat(actual.blockNumber).isNull()
        assertThat(actual.logIndex).isNull()
        assertThat(actual.blockchain).isEqualTo(source.id.blockchain)
        assertThat(actual.type).isEqualTo(ActivityTypeDto.AUCTION_CREATED)
        assertThat(actual.user.maker).isEqualTo(source.auction.seller.value)
        assertThat(actual.user.taker).isNull()
        assertThat(actual.collection.make).isEqualTo(source.auction.sell.type.ext.contract)
        assertThat(actual.collection.take).isEqualTo(source.auction.buy.ext.contract)
        assertThat(actual.item.make).isEqualTo(source.auction.sell.type.ext.itemId!!.value)
        assertThat(actual.item.take).isEqualTo(source.auction.buy.ext.itemId!!.value)
    }

    @Test
    fun `should convert AuctionBidActivityDto`() {
        // given
        val source = AuctionBidActivityDto(
            id = randomActivityId(),
            date = randomDate(),
            auction = randomAuction(),
            transactionHash = randomString(),
            bid = randomAuctionBidData(),
        )

        // when
        val actual = converter.convert(source)

        // then
        assertThat(actual.activityId).isEqualTo(source.id.value)
        assertThat(actual.date).isEqualTo(source.date)
        assertThat(actual.blockNumber).isNull()
        assertThat(actual.logIndex).isNull()
        assertThat(actual.blockchain).isEqualTo(source.id.blockchain)
        assertThat(actual.type).isEqualTo(ActivityTypeDto.AUCTION_BID)
        assertThat(actual.user.maker).isEqualTo(source.auction.seller.value)
        assertThat(actual.user.taker).isEqualTo(source.bid.buyer.value)
        assertThat(actual.collection.make).isEqualTo(source.auction.sell.type.ext.contract)
        assertThat(actual.collection.take).isEqualTo(source.auction.buy.ext.contract)
        assertThat(actual.item.make).isEqualTo(source.auction.sell.type.ext.itemId!!.value)
        assertThat(actual.item.take).isEqualTo(source.auction.buy.ext.itemId!!.value)
    }

    @Test
    fun `should convert AuctionFinishActivityDto`() {
        // given
        val source = AuctionFinishActivityDto(
            id = randomActivityId(),
            date = randomDate(),
            auction = randomAuction(),
            transactionHash = randomString(),
        )

        // when
        val actual = converter.convert(source)

        // then
        assertThat(actual.activityId).isEqualTo(source.id.value)
        assertThat(actual.date).isEqualTo(source.date)
        assertThat(actual.blockNumber).isNull()
        assertThat(actual.logIndex).isNull()
        assertThat(actual.blockchain).isEqualTo(source.id.blockchain)
        assertThat(actual.type).isEqualTo(ActivityTypeDto.AUCTION_FINISHED)
        assertThat(actual.user.maker).isEqualTo(source.auction.seller.value)
        assertThat(actual.user.taker).isNull()
        assertThat(actual.collection.make).isEqualTo(source.auction.sell.type.ext.contract)
        assertThat(actual.collection.take).isEqualTo(source.auction.buy.ext.contract)
        assertThat(actual.item.make).isEqualTo(source.auction.sell.type.ext.itemId!!.value)
        assertThat(actual.item.take).isEqualTo(source.auction.buy.ext.itemId!!.value)
    }

    @Test
    fun `should convert AuctionCancelActivityDto`() {
        // given
        val source = AuctionCancelActivityDto(
            id = randomActivityId(),
            date = randomDate(),
            auction = randomAuction(),
            transactionHash = randomString(),
        )

        // when
        val actual = converter.convert(source)

        // then
        assertThat(actual.activityId).isEqualTo(source.id.value)
        assertThat(actual.date).isEqualTo(source.date)
        assertThat(actual.blockNumber).isNull()
        assertThat(actual.logIndex).isNull()
        assertThat(actual.blockchain).isEqualTo(source.id.blockchain)
        assertThat(actual.type).isEqualTo(ActivityTypeDto.AUCTION_CANCEL)
        assertThat(actual.user.maker).isEqualTo(source.auction.seller.value)
        assertThat(actual.user.taker).isNull()
        assertThat(actual.collection.make).isEqualTo(source.auction.sell.type.ext.contract)
        assertThat(actual.collection.take).isEqualTo(source.auction.buy.ext.contract)
        assertThat(actual.item.make).isEqualTo(source.auction.sell.type.ext.itemId!!.value)
        assertThat(actual.item.take).isEqualTo(source.auction.buy.ext.itemId!!.value)
    }

    @Test
    fun `should convert AuctionStartActivityDto`() {
        // given
        val source = AuctionStartActivityDto(
            id = randomActivityId(),
            date = randomDate(),
            auction = randomAuction(),
        )

        // when
        val actual = converter.convert(source)

        // then
        assertThat(actual.activityId).isEqualTo(source.id.value)
        assertThat(actual.date).isEqualTo(source.date)
        assertThat(actual.blockNumber).isNull()
        assertThat(actual.logIndex).isNull()
        assertThat(actual.blockchain).isEqualTo(source.id.blockchain)
        assertThat(actual.type).isEqualTo(ActivityTypeDto.AUCTION_STARTED)
        assertThat(actual.user.maker).isEqualTo(source.auction.seller.value)
        assertThat(actual.user.taker).isNull()
        assertThat(actual.collection.make).isEqualTo(source.auction.sell.type.ext.contract)
        assertThat(actual.collection.take).isEqualTo(source.auction.buy.ext.contract)
        assertThat(actual.item.make).isEqualTo(source.auction.sell.type.ext.itemId!!.value)
        assertThat(actual.item.take).isEqualTo(source.auction.buy.ext.itemId!!.value)
    }

    @Test
    fun `should convert AuctionEndActivityDto`() {
        // given
        val source = AuctionEndActivityDto(
            id = randomActivityId(),
            date = randomDate(),
            auction = randomAuction(),
        )

        // when
        val actual = converter.convert(source)

        // then
        assertThat(actual.activityId).isEqualTo(source.id.value)
        assertThat(actual.date).isEqualTo(source.date)
        assertThat(actual.blockNumber).isNull()
        assertThat(actual.logIndex).isNull()
        assertThat(actual.blockchain).isEqualTo(source.id.blockchain)
        assertThat(actual.type).isEqualTo(ActivityTypeDto.AUCTION_ENDED)
        assertThat(actual.user.maker).isEqualTo(source.auction.seller.value)
        assertThat(actual.user.taker).isNull()
        assertThat(actual.collection.make).isEqualTo(source.auction.sell.type.ext.contract)
        assertThat(actual.collection.take).isEqualTo(source.auction.buy.ext.contract)
        assertThat(actual.item.make).isEqualTo(source.auction.sell.type.ext.itemId!!.value)
        assertThat(actual.item.take).isEqualTo(source.auction.buy.ext.itemId!!.value)
    }

    private fun randomActivityId(): ActivityIdDto {
        return ActivityIdDto(
            blockchain = randomBlockchain(),
            value = randomAddress().toString()
        )
    }

    private fun randomDate(): Instant {
        return Instant.ofEpochMilli(randomLong())
    }

    private fun randomBlockchain(): BlockchainDto {
        return BlockchainDto.values().random()
    }

    private fun randomUnionAddress(): UnionAddress {
        return UnionAddress(
            blockchainGroup = randomBlockchainGroup(),
            value = randomAddress().toString()
        )
    }

    private fun randomItemId(): ItemIdDto {
        return ItemIdDto(
            blockchain = randomBlockchain(),
            contract = randomString(),
            tokenId = randomBigInt()
        )
    }

    private fun randomBlockchainGroup(): BlockchainGroupDto {
        return BlockchainGroupDto.values().random()
    }

    private fun randomOrderMatchSide(): OrderActivityMatchSideDto {
        return OrderActivityMatchSideDto(
            maker = randomUnionAddress(),
            hash = randomString(),
            asset = randomAsset()
        )
    }

    private fun randomAsset(): AssetDto {
        return AssetDto(
            type = randomAssetType(),
            value = randomBigDecimal(),
        )
    }

    private fun randomAssetType(): AssetTypeDto {
        return EthErc1155AssetTypeDto(
            contract = ContractAddress(
                randomBlockchain(),
                randomString()
            ),
            tokenId = randomBigInt(),
        )
    }

    private fun randomAuction(): AuctionDto {
        return AuctionDto(
            id = AuctionIdDto(
                randomBlockchain(),
                randomString(),
            ),
            contract = ContractAddress(
                randomBlockchain(),
                randomString(),
            ),
            type = AuctionDto.Type.values().random(),
            seller = randomUnionAddress(),
            sell = randomAsset(),
            buy = randomAssetType(),
            endTime = randomDate(),
            minimalStep = randomBigDecimal(),
            minimalPrice = randomBigDecimal(),
            createdAt = randomDate(),
            lastUpdateAt = randomDate(),
            buyPrice = randomBigDecimal(),
            buyPriceUsd = randomBigDecimal(),
            pending = listOf(AuctionHistoryDto(randomString())),
            status = AuctionStatusDto.values().random(),
            ongoing = randomBoolean(),
            hash = randomString(),
            auctionId = randomBigInt(),
            lastBid = null,
            data = randomAuctionData()
        )
    }

    private fun randomAuctionBidData(): AuctionBidDto {
        return RaribleAuctionV1BidV1Dto(
            buyer = randomUnionAddress(),
            amount = randomBigDecimal(),
            date = randomDate(),
            status = AuctionBidDto.Status.values().random(),
            data = RaribleAuctionV1BidDataV1Dto(
                originFees = emptyList(),
                payouts = emptyList(),
            )
        )
    }

    private fun randomAuctionData(): AuctionDataDto {
        return RaribleAuctionV1DataV1Dto(
            originFees = emptyList(),
            payouts = emptyList(),
            startTime = randomDate(),
            duration = randomBigInt(),
            buyOutPrice = randomBigDecimal(),
        )
    }
}