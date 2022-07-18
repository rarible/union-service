package com.rarible.protocol.union.api.controller

import com.rarible.core.logging.Logger
import com.rarible.protocol.union.api.service.select.ActivitySourceSelectService
import com.rarible.protocol.union.api.service.select.OverrideSelect
import com.rarible.protocol.union.core.exception.UnionException
import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.SyncTypeDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import java.time.Instant
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class ActivityController(
    private val activitySourceSelector: ActivitySourceSelectService
) : ActivityControllerApi {

    companion object {
        private val logger by Logger()
    }

    override suspend fun getAllActivities(
        type: List<ActivityTypeDto>,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?,
        newSearchEngine: Boolean?
    ): ResponseEntity<ActivitiesDto> {
        logger.info("Got request to get all activities, parameters: $type, $blockchains, $continuation, $cursor, $size, $sort")
        val overrideSelect = if (newSearchEngine == true) OverrideSelect.ELASTIC else null
        val result = activitySourceSelector.getAllActivities(type, blockchains, continuation, cursor, size, sort, overrideSelect)
        return ResponseEntity.ok(result)
    }

    override suspend fun getAllActivitiesSync(
        blockchain: BlockchainDto,
        continuation: String?,
        size: Int?,
        sort: SyncSortDto?,
        type: SyncTypeDto?
    ): ResponseEntity<ActivitiesDto> {
        logger.info("Got request to get all activities sync, parameters: $blockchain, $continuation, $size, $sort")
        val result = activitySourceSelector.getAllActivitiesSync(blockchain, continuation, size, sort, type)
        return ResponseEntity.ok(result)
    }

    override suspend fun getActivitiesByCollection(
        type: List<ActivityTypeDto>,
        collection: List<String>,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?,
        newSearchEngine: Boolean?
    ): ResponseEntity<ActivitiesDto> {
        val overrideSelect = if (newSearchEngine == true) OverrideSelect.ELASTIC else null
        if (collection.isEmpty()) throw UnionException("No any collection param in query")
        val result = activitySourceSelector.getActivitiesByCollection(type, collection.first() /** todo support string collection */, continuation, cursor, size, sort, overrideSelect)
        return ResponseEntity.ok(result)
    }


    override suspend fun getActivitiesByItem(
        type: List<ActivityTypeDto>,
        itemId: String,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?,
        newSearchEngine: Boolean?
    ): ResponseEntity<ActivitiesDto> {
        val overrideSelect = if (newSearchEngine == true) OverrideSelect.ELASTIC else null
        val result = activitySourceSelector.getActivitiesByItem(type, itemId, continuation, cursor, size, sort, overrideSelect)
        return ResponseEntity.ok(result)
    }

    override suspend fun getActivitiesByUser(
        type: List<UserActivityTypeDto>,
        user: List<String>,
        blockchains: List<BlockchainDto>?,
        from: Instant?,
        to: Instant?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?,
        newSearchEngine: Boolean?
    ): ResponseEntity<ActivitiesDto> {
        val overrideSelect = if (newSearchEngine == true) OverrideSelect.ELASTIC else null
        val result = activitySourceSelector.getActivitiesByUser(type, user, blockchains, from, to, continuation, cursor, size, sort, overrideSelect)
        return ResponseEntity.ok(result)
    }
}
