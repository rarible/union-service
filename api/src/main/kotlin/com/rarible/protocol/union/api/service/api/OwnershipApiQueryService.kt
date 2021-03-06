package com.rarible.protocol.union.api.service.api

import com.rarible.protocol.union.api.service.EnrichedOwnershipApiHelper
import com.rarible.protocol.union.api.service.OwnershipQueryService
import com.rarible.protocol.union.core.continuation.UnionAuctionOwnershipWrapperContinuation
import com.rarible.protocol.union.core.continuation.UnionOwnershipContinuation
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipSearchRequestDto
import com.rarible.protocol.union.dto.OwnershipsDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.page.Paging
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.service.EnrichmentAuctionService
import org.springframework.stereotype.Component

@Component
class OwnershipApiQueryService(
    private val enrichmentAuctionService: EnrichmentAuctionService,
    private val apiHelper: EnrichedOwnershipApiHelper,
) : OwnershipQueryService {

    override suspend fun getOwnershipById(fullOwnershipId: OwnershipIdDto): OwnershipDto =
        apiHelper.getOwnershipById(fullOwnershipId)

    override suspend fun getOwnershipsByIds(ids: List<OwnershipIdDto>): List<OwnershipDto> {
        return apiHelper.getOwnershipsByIds(ids)
    }

    override suspend fun getOwnershipByOwner(
        owner: UnionAddress,
        continuation: String?,
        size: Int,
    ): Slice<UnionOwnership> {
        return apiHelper.getEnrichedOwnerships(
            continuation,
            size,
            { enrichmentAuctionService.findBySeller(owner) },
            { apiHelper.getRawOwnershipsByOwner(owner, continuation, it) },
            { ownerships ->
                Paging(
                    UnionOwnershipContinuation.ByLastUpdatedAndId,
                    apiHelper.merge(ownerships),
                ).getSlice(size)
            }
        )
    }

    override suspend fun getOwnershipsByItem(
        itemId: ItemIdDto,
        continuation: String?,
        size: Int,
    ): OwnershipsDto {
        return apiHelper.getEnrichedOwnerships(
            continuation,
            size,
            { enrichmentAuctionService.findByItem(ShortItemId(itemId)) },
            { apiHelper.getRawOwnershipsByItem(itemId, continuation, it) },
            { ownerships ->
                val page =
                    Paging(UnionAuctionOwnershipWrapperContinuation.ByLastUpdatedAndId, ownerships).getSlice(size)
                OwnershipsDto(page.entities.size.toLong(), page.continuation, apiHelper.enrich(page.entities))
            }
        )
    }

    override suspend fun search(request: OwnershipSearchRequestDto): OwnershipsDto {
        throw UnsupportedOperationException("Not supported for API query service")
    }
}
