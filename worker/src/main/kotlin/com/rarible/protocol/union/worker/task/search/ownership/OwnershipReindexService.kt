package com.rarible.protocol.union.worker.task.search.ownership

import com.rarible.protocol.union.core.converter.EsOwnershipConverter
import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.worker.metrics.SearchTaskMetricFactory
import com.rarible.protocol.union.worker.task.search.OwnershipTaskParam
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.elasticsearch.action.support.WriteRequest
import org.springframework.stereotype.Service

@Service
class OwnershipReindexService(
    private val repository: EsOwnershipRepository,
    private val searchTaskMetricFactory: SearchTaskMetricFactory,
    private val rawOwnershipClient: RawOwnershipClient,
) {

    fun reindex(
        blockchain: BlockchainDto,
        target: OwnershipTaskParam.Target,
        index: String?,
        cursor: String? = null,
    ): Flow<String> = when (target) {
        OwnershipTaskParam.Target.OWNERSHIP -> reindexOwnerships(blockchain, target, index, cursor)
        OwnershipTaskParam.Target.AUCTIONED_OWNERSHIP -> reindexAuctionedOwnership(blockchain, target, index, cursor)
    }

    private fun reindexAuctionedOwnership(
        blockchain: BlockchainDto,
        target: OwnershipTaskParam.Target,
        index: String?,
        cursor: String?,
    ): Flow<String> = doReindex(blockchain, target, index, cursor, EsOwnershipConverter::convert) {
        rawOwnershipClient.getAuctionAll(blockchain, it, PageSize.OWNERSHIP.max)
    }

    private fun reindexOwnerships(
        blockchain: BlockchainDto,
        target: OwnershipTaskParam.Target,
        index: String?,
        cursor: String?,
    ): Flow<String> = doReindex(blockchain, target, index, cursor, EsOwnershipConverter::convert) {
        // TODO read values from config
        val size = when (blockchain) {
            BlockchainDto.SOLANA -> 250
            else -> PageSize.OWNERSHIP.max
        }
        rawOwnershipClient.getRawOwnershipsAll(blockchain, it, size)
    }

    private fun <T> doReindex(
        blockchain: BlockchainDto,
        target: OwnershipTaskParam.Target,
        index: String?,
        cursor: String?,
        convert: (T) -> EsOwnership,
        job: suspend (String?) -> Slice<T>,
    ) = flow {
        val counter = searchTaskMetricFactory.createReindexOwnershipCounter(blockchain, target)
        var current: String? = cursor
        do {
            val result = job(current)
            current = result.continuation

            val ownerships = result.entities.map { convert(it) }
            val saved = repository.saveAll(ownerships, index, WriteRequest.RefreshPolicy.NONE)
            counter.increment(saved.size)

            emit(current.orEmpty())
        } while (current != null)
    }
}
