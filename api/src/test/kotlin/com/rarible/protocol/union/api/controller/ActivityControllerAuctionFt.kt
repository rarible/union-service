package com.rarible.protocol.union.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.ActivitySortDto
import com.rarible.protocol.dto.AuctionActivitiesDto
import com.rarible.protocol.dto.AuctionActivityDto
import com.rarible.protocol.dto.NftActivitiesDto
import com.rarible.protocol.dto.OrderActivitiesDto
import com.rarible.protocol.union.api.client.ActivityControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.AuctionBidActivityDto
import com.rarible.protocol.union.dto.AuctionCancelActivityDto
import com.rarible.protocol.union.dto.AuctionStartActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.integration.ethereum.data.randomEthAddress
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionBidActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionCancelActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionFinishActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionOpenActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionStartActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.coEvery
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono

@FlowPreview
@IntegrationTest
class ActivityControllerAuctionFt : AbstractIntegrationTest() {

    private val continuation: String? = null
    private val size = PageSize.ACTIVITY.default
    private val sort: com.rarible.protocol.union.dto.ActivitySortDto? = null

    @Autowired
    lateinit var activityControllerApi: ActivityControllerApi

    @Test
    fun `get all auctions activities - desc`() = runBlocking<Unit> {
        val types = ActivityTypeDto.values().toList()
        val blockchains = listOf(BlockchainDto.ETHEREUM)
        val now = nowMillis()
        val list = listOf(
            randomEthAuctionFinishActivity().copy(date = now.plusSeconds(5)),
            randomEthAuctionCancelActivity().copy(date = now.plusSeconds(4)),
            randomEthAuctionCancelActivity().copy(date = now.plusSeconds(3)),
            randomEthAuctionBidActivity().copy(date = now.plusSeconds(2)),
            randomEthAuctionStartActivity().copy(date = now.plusSeconds(1)),
            randomEthAuctionOpenActivity().copy(date = now)
        )

        coEvery {
            testEthereumActivityItemApi.getNftActivities(any(), isNull(), eq(size), ActivitySortDto.LATEST_FIRST)
        } returns NftActivitiesDto(null, emptyList()).toMono()

        coEvery {
            testEthereumActivityOrderApi.getOrderActivities(any(), isNull(), eq(size), ActivitySortDto.LATEST_FIRST)
        } returns OrderActivitiesDto(null, emptyList()).toMono()

        coEvery {
            testEthereumActivityAuctionApi.getAuctionActivities(any(), isNull(), eq(size), ActivitySortDto.LATEST_FIRST)
        } returns AuctionActivitiesDto(null, list).toMono()

        val activities = activityControllerApi.getAllActivities(
            types, blockchains, null, null, size, sort, null,
        ).awaitFirst()

        checkActivities(list, activities.activities)
    }

    @Test
    fun `get auctions activities by collection - ethereum`() = runBlocking<Unit> {
        val types = listOf(ActivityTypeDto.AUCTION_STARTED)
        val ethCollectionId = CollectionIdDto(BlockchainDto.ETHEREUM, randomEthAddress())
        val auctionActivity = randomEthAuctionStartActivity()

        coEvery {
            testEthereumActivityAuctionApi.getAuctionActivities(any(), isNull(), eq(size), ActivitySortDto.LATEST_FIRST)
        } returns AuctionActivitiesDto(null, listOf(auctionActivity)).toMono()

        val activities = activityControllerApi.getActivitiesByCollection(
            types, listOf(ethCollectionId.fullId()), null, null, size, sort, null,
        ).awaitFirst()

        assertThat(activities.activities).hasSize(1)
        assertThat(activities.activities[0]).isInstanceOf(AuctionStartActivityDto::class.java)
    }

    @Test
    fun `get auctions activities by item - ethereum`() = runBlocking<Unit> {
        val types = listOf(ActivityTypeDto.AUCTION_CANCEL)
        val ethItemId = randomEthItemId()
        val auctionActivity = randomEthAuctionCancelActivity()

        coEvery {
            testEthereumActivityAuctionApi.getAuctionActivities(any(), isNull(), eq(size), ActivitySortDto.LATEST_FIRST)
        } returns AuctionActivitiesDto(null, listOf(auctionActivity)).toMono()

        val activities = activityControllerApi.getActivitiesByItem(
            types, ethItemId.fullId(), null, null, size, sort, null,
        ).awaitFirst()

        assertThat(activities.activities).hasSize(1)
        assertThat(activities.activities[0]).isInstanceOf(AuctionCancelActivityDto::class.java)
    }

    @Test
    fun `get activities by user`() = runBlocking<Unit> {
        val types = listOf(UserActivityTypeDto.MAKE_BID)
        val userEth = UnionAddressConverter.convert(BlockchainDto.ETHEREUM, randomEthAddress())
        val auctionActivity = randomEthAuctionBidActivity()

        coEvery {
            testEthereumActivityOrderApi.getOrderActivities(any(), isNull(), eq(size), ActivitySortDto.LATEST_FIRST)
        } returns OrderActivitiesDto(null, emptyList()).toMono()

        coEvery {
            testEthereumActivityAuctionApi.getAuctionActivities(any(), isNull(), eq(size), ActivitySortDto.LATEST_FIRST)
        } returns AuctionActivitiesDto(null, listOf(auctionActivity)).toMono()

        coEvery {
            testPolygonActivityOrderApi.getOrderActivities(any(), isNull(), eq(size), ActivitySortDto.LATEST_FIRST)
        } returns OrderActivitiesDto(null, emptyList()).toMono()

        coEvery {
            testPolygonActivityAuctionApi.getAuctionActivities(any(), isNull(), eq(size), ActivitySortDto.LATEST_FIRST)
        } returns AuctionActivitiesDto(null, emptyList()).toMono()

        val now = Instant.now()
        val oneWeekAgo = now.minus(7, ChronoUnit.DAYS)
        val activities = activityControllerApi.getActivitiesByUser(
            types, listOf(userEth.fullId()), null, oneWeekAgo, now, null, null, size, sort, null,
        ).awaitFirst()

        assertThat(activities.activities).hasSize(1)
        assertThat(activities.activities[0]).isInstanceOf(AuctionBidActivityDto::class.java)
    }

    private fun checkActivities(list: List<AuctionActivityDto>, activities: List<ActivityDto>) {
        assertThat(activities).hasSize(list.size)
        list.forEachIndexed { i, activity ->
            assertThat(activities[i].id.value).isEqualTo(activity.id)
        }
    }
}
