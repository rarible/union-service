package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.common.mapAsync
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.api.service.ItemEnrichService
import com.rarible.protocol.union.api.service.ItemQueryService
import com.rarible.protocol.union.core.continuation.UnionItemContinuation
import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsItemCursor
import com.rarible.protocol.union.core.model.EsItemQueryResult
import com.rarible.protocol.union.core.model.EsItemSort
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.ItemsWithOwnershipDto
import com.rarible.protocol.union.dto.continuation.page.ArgPage
import com.rarible.protocol.union.dto.continuation.page.ArgPaging
import com.rarible.protocol.union.dto.continuation.page.ArgSlice
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Service

@Service
@CaptureSpan(type = SpanType.APP)
@ExperimentalCoroutinesApi
class ItemElasticService(
    private val itemFilterConverter: ItemFilterConverter,
    private val esItemRepository: EsItemRepository,
    private val router: BlockchainRouter<ItemService>,
    private val itemEnrichService: ItemEnrichService,
) : ItemQueryService {

    companion object {
        private val logger by Logger()
    }

    override suspend fun getAllItems(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?
    ): ItemsDto {

        val safeSize = PageSize.ITEM.limit(size)
        val slices = getAllItemsInner(blockchains, showDeleted, lastUpdatedFrom, lastUpdatedTo, continuation, size)
        val total = slices.sumOf { it.page.total }
        val arg = ArgPaging(UnionItemContinuation.ByLastUpdatedAndId, slices.map { it.toSlice() }).getSlice(safeSize)

        logger.info("Response for getAllItems(blockchains={}, continuation={}, size={}):" +
            " Page(size={}, total={}, continuation={}) from blockchain pages {} ",
            blockchains, continuation, safeSize, arg.entities.size, total,
            arg.continuation, slices.map { it.page.entities.size }
        )
        return itemEnrichService.enrich(arg, total)
    }

    private suspend fun getAllItemsInner(
        blockchains: List<BlockchainDto>?,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?,
        continuation: String?,
        size: Int?
    ): List<ArgPage<UnionItem>> {
        logger.info("getAllActivities() from ElasticSearch")
        val evaluatedBlockchains = router.getEnabledBlockchains(blockchains).map { it.name }.toSet()

        val filter = itemFilterConverter.convertGetAllItems(
            evaluatedBlockchains, showDeleted, lastUpdatedFrom, lastUpdatedTo, continuation
        )
        logger.info("Built filter: $filter")
        val queryResult = esItemRepository.search(filter, EsItemSort.DEFAULT, size)
        logger.info("Query result: $queryResult")
        return getItems(queryResult.items, queryResult.cursor)
    }

    private suspend fun getItems(esItems: List<EsItem>, continuation: String?): List<ArgPage<UnionItem>> {
        if (esItems.isEmpty()) return emptyList()
        val mapping = hashMapOf<BlockchainDto, MutableList<String>>()

        esItems.forEach { item ->
            mapping
                .computeIfAbsent(item.blockchain) { ArrayList(esItems.size) }
                .add(item.itemId)
        }
        val items = mapping.mapAsync { element ->
            val blockchain = element.key
            val ids = element.value
            val isBlockchainEnabled = router.isBlockchainEnabled(blockchain)
            if (isBlockchainEnabled) {
                val page = router.getService(blockchain).getItemsByIds(ids)
                ArgPage(
                    blockchain.name,
                    "",
                    Page(0, continuation, page)
                )
            } else ArgPage(blockchain.name, null, Page(0, null, emptyList()))
        }

        return items
    }

    override suspend fun getItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int?
    ): ItemsDto {
        val safeSize = PageSize.ITEM.limit(size)
        val collectionId = IdParser.parseCollectionId(collection)

        val cursor = continuation?.let { EsItemCursor(itemId = it) }
        val filter = itemFilterConverter.getItemsByCollection(collection, cursor)
        logger.info("Built filter: $filter")
        val queryResult = esItemRepository.search(filter, EsItemSort.DEFAULT, safeSize)
        logger.info("Query result: $queryResult")
        val ids = queryResult.items.map { it.itemId }
        val result: List<UnionItem> = router.getService(collectionId.blockchain).getItemsByIds(ids)

        logger.info(
            "Response for getItemsByCollection(collection={}, continuation={}, size={}):" +
                " Page(size={}, continuation={})",
            collection, continuation, size, queryResult.items.size, queryResult.cursor
        )

        return itemEnrichService.enrich(result, queryResult.cursor, queryResult.total)
    }

    override suspend fun getAllItemIdsByCollection(collectionId: CollectionIdDto): Flow<ItemIdDto> {
        val pageSize = PageSize.ITEM.max
        var cursor: EsItemCursor? = null
        var returned = 0L
        return flow {
            while (true) {
                val filter = itemFilterConverter.getAllItemIdsByCollection(collectionId.fullId(), cursor)
                logger.info("Built filter: $filter")
                val queryResult = esItemRepository.search(filter, EsItemSort.DEFAULT, pageSize)
                logger.info("Query result: $queryResult")
                queryResult.items.forEach { emit(IdParser.parseItemId(it.itemId)) }
                cursor = EsItemCursor(itemId = queryResult.cursor)
                if (queryResult.cursor == ArgSlice.COMPLETED || queryResult.items.isEmpty()) break
                returned += queryResult.items.size
                check(returned < 1_000_000) { "Cyclic continuation ${queryResult.cursor} for collection $collectionId" }
            }
        }
    }

    override suspend fun getItemsByCreator(
        creator: String,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ItemsDto {
        val safeSize = PageSize.ITEM.limit(size)
        val creatorAddress = IdParser.parseAddress(creator)

        val cursor = continuation?.let { EsItemCursor(itemId = it) }
        val filter = itemFilterConverter.getItemsByCreator(creatorAddress.fullId(), cursor)
        logger.info("Built filter: $filter")
        val queryResult = esItemRepository.search(filter, EsItemSort.DEFAULT, safeSize)
        logger.info("Query result: $queryResult")

        if (queryResult.items.isEmpty()) return ItemsDto()
        val items = getItems(queryResult)

        return itemEnrichService.enrich(items, queryResult.cursor, queryResult.total)
    }

    private suspend fun getItems(queryResult: EsItemQueryResult): List<UnionItem> {
        val mapping = hashMapOf<BlockchainDto, MutableList<String>>()

        queryResult.items.forEach { item ->
            mapping
                .computeIfAbsent(item.blockchain) { ArrayList(queryResult.items.size) }
                .add(item.itemId)
        }

        val items = mapping.mapAsync { element ->
            val blockchain = element.key
            val ids = element.value
            val isBlockchainEnabled = router.isBlockchainEnabled(blockchain)
            if (isBlockchainEnabled) {
                router.getService(blockchain).getItemsByIds(ids)
            } else emptyList()
        }.flatten()
        return items
    }

    override suspend fun getItemsByOwner(
        owner: String,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ItemsDto {
        val evaluatedBlockchains = router.getEnabledBlockchains(blockchains).map { it.name }.toSet()
        return getItemsByOwnerInner(owner, evaluatedBlockchains, continuation, size)
    }

    private suspend fun getItemsByOwnerInner(
        owner: String,
        evaluatedBlockchains: Set<String>?,
        continuation: String?,
        size: Int?
    ): ItemsDto {
        val safeSize = PageSize.ITEM.limit(size)
        val ownerAddress = IdParser.parseAddress(owner)
        val cursor = continuation?.let { EsItemCursor(itemId = it) }
        val filter = itemFilterConverter.getItemsByOwner(ownerAddress.fullId(), evaluatedBlockchains, cursor)
        logger.info("Built filter: $filter")
        val queryResult = esItemRepository.search(filter, EsItemSort.DEFAULT, safeSize)
        logger.info("Query result: $queryResult")

        if (queryResult.items.isEmpty()) return ItemsDto()
        val items = getItems(queryResult)

        return itemEnrichService.enrich(items, queryResult.cursor, queryResult.total)
    }

    override suspend fun getItemsByOwnerWithOwnership(
        owner: String,
        continuation: String?,
        size: Int?
    ): ItemsWithOwnershipDto {
        throw NotImplementedError()
    }

    override suspend fun getItemsByIds(ids: List<ItemIdDto>): List<ItemDto> {
        throw NotImplementedError()
    }
}