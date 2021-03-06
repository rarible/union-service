package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexMintsPage
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexTradesPage
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexTransfersPage
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant

class ImmutablexActivityClient(
    webClient: WebClient,
) : AbstractImmutablexClient(
    webClient
) {

    // TODO IMMUTABLEX move out to configuration
    private val creatorsRequestChunkSize = 16

    suspend fun getMints(
        pageSize: Int,
        continuation: String? = null,
        token: String? = null,
        tokenId: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        user: String? = null,
        sort: ActivitySortDto? = null
    ) = getActivities<ImmutablexMintsPage>(
        pageSize,
        continuation,
        token,
        tokenId,
        from,
        to,
        user,
        sort,
        ActivityType.MINT
    ) ?: ImmutablexMintsPage.EMPTY

    suspend fun getTransfers(
        pageSize: Int,
        continuation: String? = null,
        token: String? = null,
        tokenId: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        user: String? = null,
        sort: ActivitySortDto?
    ) = getActivities<ImmutablexTransfersPage>(
        pageSize,
        continuation,
        token,
        tokenId,
        from,
        to,
        user,
        sort,
        ActivityType.TRANSFER
    ) ?: ImmutablexTransfersPage.EMPTY

    suspend fun getTrades(
        pageSize: Int,
        continuation: String? = null,
        token: String? = null,
        tokenId: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        user: String? = null,
        sort: ActivitySortDto?
    ) = getActivities<ImmutablexTradesPage>(
        pageSize,
        continuation,
        token,
        tokenId,
        from,
        to,
        user,
        sort,
        ActivityType.TRADE
    ) ?: ImmutablexTradesPage.EMPTY

    private suspend inline fun <reified T> getActivities(
        pageSize: Int,
        continuation: String?,
        token: String? = null,
        tokenId: String? = null,
        from: Instant?,
        to: Instant?,
        user: String?,
        sort: ActivitySortDto?,
        type: ActivityType
    ) = webClient.get()
        .uri {
            val builder = ImmutablexActivityQueryBuilder.getQueryBuilder(type, it)
            val safeSort = sort ?: ActivitySortDto.LATEST_FIRST

            builder.token(token)
            builder.tokenId(tokenId)
            builder.user(user)
            builder.pageSize(pageSize)
            // Sorting included
            builder.continuation(from, to, safeSort, continuation)
            builder.build()
        }
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .toEntity(T::class.java).awaitSingle().body

    suspend fun getItemCreator(itemId: String): String? {
        val (token, tokenId) = IdParser.split(itemId, 2)
        return getMints(pageSize = 1, token = token, tokenId = tokenId).result.firstOrNull()?.user
    }

    suspend fun getItemCreators(assetIds: Collection<String>): Map<String, String> {
        return getChunked(creatorsRequestChunkSize, assetIds) { itemId ->
            getItemCreator(itemId)?.let { Pair(itemId, it) }
        }.associateBy({ it.first }, { it.second })
    }
}