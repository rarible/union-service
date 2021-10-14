package com.rarible.protocol.union.core.flow.service

import com.rarible.protocol.flow.nft.api.client.FlowNftOrderActivityControllerApi
import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.flow.converter.FlowActivityConverter
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import kotlinx.coroutines.reactive.awaitFirst
import java.time.Instant

class FlowActivityService(
    blockchain: BlockchainDto,
    private val activityControllerApi: FlowNftOrderActivityControllerApi,
    private val flowActivityConverter: FlowActivityConverter
) : AbstractBlockchainService(blockchain), ActivityService {

    override suspend fun getAllActivities(
        types: List<ActivityTypeDto>,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val rawTypes = types.map { it.name }
        val result = activityControllerApi.getNftOrderAllActivities(rawTypes, continuation, size)
            .awaitFirst()
        return flowActivityConverter.convert(result, blockchain)
    }

    override suspend fun getActivitiesByCollection(
        types: List<ActivityTypeDto>,
        collection: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val rawTypes = types.map { it.name }
        val result = activityControllerApi.getNftOrderActivitiesByCollection(rawTypes, collection, continuation, size)
            .awaitFirst()
        return flowActivityConverter.convert(result, blockchain)
    }

    override suspend fun getActivitiesByItem(
        types: List<ActivityTypeDto>,
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val rawTypes = types.map { it.name }
        val result = activityControllerApi.getNftOrderActivitiesByItem(
            rawTypes,
            contract,
            tokenId.toLong(),
            continuation,
            size
        ).awaitFirst()
        return flowActivityConverter.convert(result, blockchain)
    }

    override suspend fun getActivitiesByUser(
        types: List<UserActivityTypeDto>,
        users: List<String>,
        from: Instant?,
        to: Instant?,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val rawTypes = types.map { it.name }

        val result = activityControllerApi
            .getNftOrderActivitiesByUser(
                rawTypes, users, from?.toEpochMilli(), to?.toEpochMilli(), continuation, size, sort?.name
            )
            .awaitFirst()
        return flowActivityConverter.convert(result, blockchain)
    }
}
