package com.rarible.protocol.union.integration.solana.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.solana.api.client.ActivityControllerApi
import com.rarible.protocol.solana.dto.ActivityFilterAllDto
import com.rarible.protocol.solana.dto.ActivityFilterByCollectionDto
import com.rarible.protocol.solana.dto.ActivityFilterByItemDto
import com.rarible.protocol.solana.dto.ActivityFilterByUserDto
import com.rarible.protocol.solana.dto.ActivityFilterDto
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.solana.converter.SolanaActivityConverter
import com.rarible.protocol.union.integration.solana.converter.SolanaConverter
import kotlinx.coroutines.reactive.awaitFirst
import java.time.Instant

@CaptureSpan(type = "blockchain")
open class SolanaActivityService(
    private val activityApi: ActivityControllerApi,
    private val activityConverter: SolanaActivityConverter
) : AbstractBlockchainService(BlockchainDto.SOLANA), ActivityService {

    override suspend fun getAllActivities(
        types: List<ActivityTypeDto>,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val solanaTypes = types.mapNotNull { activityConverter.convertToAllTypes(it) }
        if (solanaTypes.isEmpty()) return Slice.empty()

        val filter = ActivityFilterAllDto(
            types = solanaTypes
        )
        return searchActivities(filter, continuation, size, sort)
    }

    override suspend fun getActivitiesByCollection(
        types: List<ActivityTypeDto>,
        collection: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val solanaTypes = types.mapNotNull { activityConverter.convertToCollectionTypes(it) }
        if (solanaTypes.isEmpty()) return Slice.empty()

        val filter = ActivityFilterByCollectionDto(
            collection = collection,
            types = solanaTypes
        )
        return searchActivities(filter, continuation, size, sort)
    }

    override suspend fun getActivitiesByItem(
        types: List<ActivityTypeDto>,
        itemId: String,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val solanaTypes = types.mapNotNull { activityConverter.convertToItemTypes(it) }
        if (solanaTypes.isEmpty()) return Slice.empty()

        val filter = ActivityFilterByItemDto(
            itemId = itemId,
            types = solanaTypes
        )
        return searchActivities(filter, continuation, size, sort)
    }

    override suspend fun getActivitiesByUser(
        types: List<UserActivityTypeDto>,
        users: List<String>,
        from: Instant?,
        to: Instant?,
        continuation: String?,
        size: Int, sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val solanaTypes = types.mapNotNull { activityConverter.convertToUserTypes(it) }
        if (solanaTypes.isEmpty()) return Slice.empty()

        val filter = ActivityFilterByUserDto(
            users = users,
            types = solanaTypes,
            from = from,
            to = from,
        )
        return searchActivities(filter, continuation, size, sort)

    }

    private suspend fun searchActivities(
        filter: ActivityFilterDto,
        continuation: String?,
        size: Int,
        sort: ActivitySortDto?
    ): Slice<ActivityDto> {
        val solanaSort = sort?.let { SolanaConverter.convert(it) }
        val result = activityApi.searchActivities(filter, continuation, size, solanaSort)
            .awaitFirst()

        return Slice(
            result.continuation,
            result.activities.map { activityConverter.convert(it, blockchain) }
        )
    }

}