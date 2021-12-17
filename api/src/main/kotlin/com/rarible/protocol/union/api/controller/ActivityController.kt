package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.service.ActivityApiService
import com.rarible.protocol.union.api.service.extractItemId
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ActivitiesDto
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.dto.continuation.ActivityContinuation
import com.rarible.protocol.union.dto.continuation.page.ArgPaging
import com.rarible.protocol.union.dto.continuation.page.ArgSlice
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.dto.subchains
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class ActivityController(
    private val router: BlockchainRouter<ActivityService>,
    private val activityApiService: ActivityApiService
) : ActivityControllerApi {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun getAllActivities(
        type: List<ActivityTypeDto>,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<ActivitiesDto> {
        val safeSize = PageSize.ACTIVITY.limit(size)
        val (result, slicesCounter) = if (null == cursor) {
            val evaluatedBlockchains = router.getEnabledBlockchains(blockchains)
            val blockchainMap = coroutineScope {
                evaluatedBlockchains.map {
                    it to async {
                        router.getService(it).getAllActivities(type, continuation, safeSize, sort)
                    }
                }.toMap().mapValues { it.value.await() }
            }
            val dto = merge(blockchainMap, safeSize, sort)
            Pair(dto, blockchainMap.map { it.value.entities.size })
        } else {
            val slices = activityApiService.getAllActivities(type, blockchains, cursor, safeSize, sort)
            val dto = toDtoWithCursor(ArgPaging(continuationFactory(sort), slices).getSlice(safeSize))
            Pair(dto, slices.map { it.slice.entities.size })
        }
        logger.info(
            "Response for getAllActivities(type={}, blockchains={}, continuation={}, size={}, sort={}):" +
                    " Slice(size={}, continuation={}, cursor={}) from blockchain slices {} ",
            type,
            blockchains,
            continuation,
            size,
            sort,
            result.activities.size,
            result.continuation,
            result.cursor,
            slicesCounter
        )
        return ResponseEntity.ok(result)
    }

    override suspend fun getActivitiesByCollection(
        type: List<ActivityTypeDto>,
        collection: String,
        continuation: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<ActivitiesDto> {
        val safeSize = PageSize.ACTIVITY.limit(size)
        val collectionAddress = IdParser.parseContract(collection)
        val result = router.getService(collectionAddress.blockchain)
            .getActivitiesByCollection(type, collectionAddress.value, continuation, safeSize, sort)

        logger.info(
            "Response for getActivitiesByCollection(type={}, collection={}, continuation={}, size={}, sort={}): " +
                    "Slice(size={}, continuation={}) ",
            type, collection, continuation, size, sort, result.entities.size, result.continuation
        )
        return ResponseEntity.ok(toDto(result))
    }

    override suspend fun getActivitiesByItem(
        type: List<ActivityTypeDto>,
        itemId: String?,
        contract: String?,
        tokenId: String?,
        continuation: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<ActivitiesDto> {
        val safeSize = PageSize.ACTIVITY.limit(size)
        val fullItemId = extractItemId(contract, tokenId, itemId)
        val result = router.getService(fullItemId.blockchain)
            .getActivitiesByItem(
                type,
                fullItemId.contract,
                fullItemId.tokenId.toString(),
                continuation,
                safeSize,
                sort
            )

        logger.info(
            "Response for getActivitiesByItem(type={}, itemId={} continuation={}, size={}, sort={}): " +
                    "Slice(size={}, continuation={}) ",
            type, fullItemId.fullId(), continuation, size, sort, result.entities.size, result.continuation
        )

        return ResponseEntity.ok(toDto(result))
    }

    override suspend fun getActivitiesByUser(
        type: List<UserActivityTypeDto>,
        user: List<String>,
        from: Instant?,
        to: Instant?,
        continuation: String?,
        cursor: String?,
        size: Int?,
        sort: ActivitySortDto?
    ): ResponseEntity<ActivitiesDto> {
        val safeSize = PageSize.ACTIVITY.limit(size)
        val groupedByBlockchain = user.map { IdParser.parseAddress(it) }
            // Since user specified here with blockchain group, we need to route request to all subchains
            .flatMap { address -> address.blockchainGroup.subchains().map { it to address.value } }
            .groupBy({ it.first }, { it.second })

        val (result, slicesCounter) = if (null == cursor) {
            val blockchainMap = coroutineScope {
                groupedByBlockchain.mapValues {
                    val blockchain = it.key
                    val blockchainUsers = it.value
                    async {
                        router.getService(blockchain)
                            .getActivitiesByUser(type, blockchainUsers, from, to, continuation, safeSize, sort)
                    }
                }.mapValues { it.value.await() }
            }
            val result = merge(blockchainMap, safeSize, sort)
            Pair(result, blockchainMap.map { it.value.entities.size })
        } else {
            val slices =
                activityApiService.getActivitiesByUser(type, groupedByBlockchain, from, to, cursor, safeSize, sort)
            val dto = toDtoWithCursor(ArgPaging(continuationFactory(sort), slices).getSlice(safeSize))
            Pair(dto, slices.map { it.slice.entities.size })
        }
        logger.info(
            "Response for getActivitiesByUser(type={}, users={}, continuation={}, size={}, sort={}):" +
                    " Slice(size={}, continuation={}, cursor={}) from user slices {} ",
            type, user, continuation, size, sort,
            result.activities.size, result.continuation, result.cursor, slicesCounter
        )
        return ResponseEntity.ok(result)
    }

    private fun merge(
        blockchainPages: Map<BlockchainDto, Slice<ActivityDto>>,
        size: Int,
        sort: ActivitySortDto?
    ): ActivitiesDto {
        val factory = continuationFactory(sort)
        val slices = blockchainPages.map { ArgSlice(it.key.name, it.value.continuation, it.value) }

        val legacySlice = Paging(factory, blockchainPages.values.flatMap { it.entities }).getSlice(size)
        val cursorSlice = ArgPaging(factory, slices).getSlice(size)

        return toDto(legacySlice, cursorSlice.continuation)
    }

    private fun toDto(legacySlice: Slice<ActivityDto>, cursor: String? = null): ActivitiesDto {
        return ActivitiesDto(
            continuation = legacySlice.continuation,
            cursor = cursor,
            activities = legacySlice.entities
        )
    }

    private fun toDtoWithCursor(slice: Slice<ActivityDto>): ActivitiesDto {
        return ActivitiesDto(
            continuation = null,
            // TODO remove when we get back to continuation
            cursor = slice.continuation,
            activities = slice.entities
        )
    }

    private fun continuationFactory(sort: ActivitySortDto?) = when (sort) {
        ActivitySortDto.EARLIEST_FIRST -> ActivityContinuation.ByLastUpdatedAndIdAsc
        ActivitySortDto.LATEST_FIRST, null -> ActivityContinuation.ByLastUpdatedAndIdDesc
    }
}
