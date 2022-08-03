package com.rarible.protocol.union.api.service.select

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.api.service.DiscrepancyDetector
import com.rarible.protocol.union.enrichment.service.query.activity.ActivityQueryService
import com.rarible.protocol.union.enrichment.service.query.activity.ActivityApiMergeService
import com.rarible.protocol.union.api.service.elastic.ActivityElasticService
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.SyncTypeDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ActivitySourceSelectService(
    private val featureFlagsProperties: FeatureFlagsProperties,
    private val activityApiMergeService: ActivityApiMergeService,
    private val activityElasticService: ActivityElasticService,
    private val discrepancyDetector: DiscrepancyDetector,
) {

    suspend fun getAllActivities(
        type: List<ActivityTypeDto>,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?,
        overrideSelect: OverrideSelect?,
    ): ActivitiesDto {
        return query(getQuerySource(overrideSelect, sort)) {
            it.getAllActivities(type, blockchains, continuation, cursor, size, sort)
        }
    }

    suspend fun getAllActivitiesSync(
        blockchain: BlockchainDto,
        continuation: String?,
        size: Int?,
        sort: SyncSortDto?,
        type: SyncTypeDto?
    ): ActivitiesDto {
        return activityApiMergeService.getAllActivitiesSync(blockchain, continuation, size, sort, type)
    }

    suspend fun getActivitiesByCollection(
        type: List<ActivityTypeDto>,
        collection: List<String>,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?,
        overrideSelect: OverrideSelect?,
    ): ActivitiesDto {
        return query(getQuerySource(overrideSelect, sort)) {
            it.getActivitiesByCollection(type, collection, continuation, cursor, size, sort)
        }
    }

    suspend fun getActivitiesByItem(
        type: List<ActivityTypeDto>,
        itemId: String,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?,
        overrideSelect: OverrideSelect?,
    ): ActivitiesDto {
        return query(getQuerySource(overrideSelect, sort)) {
            it.getActivitiesByItem(type, itemId, continuation, cursor, size, sort)
        }
    }

    suspend fun getActivitiesByUser(
        type: List<UserActivityTypeDto>,
        user: List<String>,
        blockchains: List<BlockchainDto>?,
        from: Instant?,
        to: Instant?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?,
        overrideSelect: OverrideSelect?,
    ): ActivitiesDto {
        return query(getQuerySource(overrideSelect, sort)) {
            it.getActivitiesByUser(type, user, blockchains, from, to, continuation, cursor, size, sort)
        }
    }

    private suspend fun query(
        chosenService: ActivityQueryService,
        call: suspend (ActivityQueryService) -> ActivitiesDto,
    ): ActivitiesDto {
        val callTime = nowMillis()
        val result = call(chosenService)

        if (chosenService is ActivityElasticService) {
            discrepancyDetector.planActivityDiscrepancyCheck(
                result,
                { call(activityApiMergeService) },
                callTime
            )
        }

        return result
    }

    private fun getQuerySource(overrideSelect: OverrideSelect?, sort: ActivitySortDto?): ActivityQueryService {
        if (overrideSelect != null) {
            return when (overrideSelect) {
                OverrideSelect.ELASTIC -> activityElasticService
                OverrideSelect.API_MERGE -> activityApiMergeService
            }
        }

        return if (featureFlagsProperties.enableActivityQueriesToElasticSearch) {
            if (featureFlagsProperties.enableActivityAscQueriesWithApiMerge) {
                if (sort == ActivitySortDto.EARLIEST_FIRST) {
                    activityApiMergeService
                } else {
                    activityElasticService
                }
            } else {
                activityElasticService
            }
        } else {
            activityApiMergeService
        }
    }
}
