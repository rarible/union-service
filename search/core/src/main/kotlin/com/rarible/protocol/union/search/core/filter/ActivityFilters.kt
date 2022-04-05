package com.rarible.protocol.union.search.core.filter

import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import java.time.Instant

sealed class ElasticActivityFilter

/**
 * ALL conditions (that aren't null/empty) must be true to pass the filter
 */
data class ElasticActivityQueryGenericFilter(
    val blockchains: Set<BlockchainDto> = emptySet(),
    val activityTypes: Set<ActivityTypeDto> = emptySet(),
    val anyUsers: Set<String> = emptySet(),
    val makers: Set<String> = emptySet(),
    val takers: Set<String> = emptySet(),
    val anyCollections: Set<String> = emptySet(),
    val makeCollections: Set<String> = emptySet(),
    val takeCollections: Set<String> = emptySet(),
    val anyItems: Set<String> = emptySet(),
    val makeItems: Set<String> = emptySet(),
    val takeItems: Set<String> = emptySet(),
    val from: Instant? = null,
    val to: Instant? = null,
    val cursor: String? = null,
) : ElasticActivityFilter()

/**
 * ALL conditions (that aren't null/empty) must be true to pass the filter,
 * also additional conditions, specific to each ActivityType, are applied
 */
data class ElasticActivityQueryPerTypeFilter(
    val blockchains: Set<BlockchainDto>,
    val from: Instant?,
    val to: Instant?,
    val cursor: String?,
    val filters: Map<ActivityTypeDto, Filter>,
) : ElasticActivityFilter() {

    data class Filter(
        val makers: Set<String>,
        val takers: Set<String>,
        val makeCollection: String?,
        val takeCollection: String?,
        val makeItem: String?,
        val takeItem: String?,
    )
}