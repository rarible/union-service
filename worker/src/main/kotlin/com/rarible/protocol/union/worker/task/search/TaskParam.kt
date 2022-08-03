package com.rarible.protocol.union.worker.task.search

import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto

sealed class TaskParam {
    abstract val versionData: Int?
    abstract val settingsHash: String?
    abstract val index: String
    abstract val blockchain: BlockchainDto
}

data class RawTaskParam(
    override val versionData: Int?,
    override val settingsHash: String?,
    override val blockchain: BlockchainDto,
    override val index: String
) : TaskParam()

/**
 * Reindexing task state
 *
 * @param blockchain blockchain
 * @param type activity type
 * @param index elasticsearch index name, including environment, version, etc.
 */
data class ActivityTaskParam(
    override val versionData: Int?,
    override val settingsHash: String?,
    override val blockchain: BlockchainDto,
    val type: ActivityTypeDto,
    override val index: String
) : TaskParam()

data class OrderTaskParam(
    override val versionData: Int?,
    override val settingsHash: String?,
    override val blockchain: BlockchainDto,
    override val index: String
) : TaskParam()

data class ItemTaskParam(
    override val versionData: Int?,
    override val settingsHash: String?,
    override val blockchain: BlockchainDto,
    override val index: String
) : TaskParam()

data class CollectionTaskParam(
    override val versionData: Int?,
    override val settingsHash: String?,
    override val blockchain: BlockchainDto,
    override val index: String
) : TaskParam()

/**
 * Reindexing task state
 *
 * @param blockchain blockchain
 * @param index elasticsearch index name, including environment, version, etc.
 */
data class OwnershipTaskParam(
    override val versionData: Int?,
    override val settingsHash: String?,
    override val blockchain: BlockchainDto,
    val target: Target,
    override val index: String,
) : TaskParam() {
    enum class Target {
        OWNERSHIP,
        AUCTIONED_OWNERSHIP,
    }
}
