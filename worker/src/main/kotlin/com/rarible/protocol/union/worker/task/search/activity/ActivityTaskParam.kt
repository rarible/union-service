package com.rarible.protocol.union.worker.task.search.activity

import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto

/**
 * Reindexing task state
 *
 * @param blockchain blockchain
 * @param activityType activity type
 * @param index elasticsearch index name, including environment, version, etc.
 */
data class ActivityTaskParam(
    val blockchain: BlockchainDto,
    val activityType: ActivityTypeDto,
    val index: String? = null,
) {
    override fun toString(): String {
        return super.toString()
    }
}