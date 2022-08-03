package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.common.mapAsync
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.converter.ItemOwnershipConverter
import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsItemSort
import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.core.model.EsOwnershipByOwnerFilter
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemWithOwnershipDto
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.ItemsSearchRequestDto
import com.rarible.protocol.union.dto.ItemsWithOwnershipDto
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.enrichment.service.query.item.ItemEnrichService
import com.rarible.protocol.union.enrichment.service.query.item.ItemQueryService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Service

@Service
@CaptureSpan(type = SpanType.APP)
class ItemElasticService(
    private val itemFilterConverter: ItemFilterConverter,
    private val esItemRepository: EsItemRepository,
    private val esOwnershipRepository: EsOwnershipRepository,
    private val ownershipElasticHelper: OwnershipElasticHelper,
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
        val slice = getAllItemsInner(blockchains, showDeleted, lastUpdatedFrom, lastUpdatedTo, continuation, size)

        logger.info("Response for getAllItems(blockchains={}, continuation={}, size={}):" +
            " Slice(size={}, continuation={})",
            blockchains, continuation, safeSize, slice.entities.size, slice.continuation
        )
        val enriched = itemEnrichService.enrich(slice.entities)

        return ItemsDto(
            items = enriched,
            continuation = slice.continuation
        )
    }

    override suspend fun getItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int?
    ): ItemsDto {
        val safeSize = PageSize.ITEM.limit(size)

        val filter = itemFilterConverter.getItemsByCollection(collection, continuation)
        logger.info("Built filter: $filter")
        val queryResult = esItemRepository.search(filter, EsItemSort.DEFAULT, safeSize)
        logger.info("Query result: $queryResult")
        val items = getItems(queryResult.entities)
        val cursor = queryResult.continuation

        logger.info(
            "Response for getItemsByCollection(collection={}, continuation={}, size={}):" +
                " Page(size={}, continuation={})",
            collection, continuation, size, items.size, cursor
        )
        val enriched = itemEnrichService.enrich(items)

        return ItemsDto(
            items = enriched,
            continuation = cursor
        )
    }

    override suspend fun getAllItemIdsByCollection(collectionId: CollectionIdDto): Flow<ItemIdDto> {
        val pageSize = PageSize.ITEM.max
        var cursor: String? = null
        var returned = 0L
        return flow {
            while (true) {
                val filter = itemFilterConverter.getAllItemIdsByCollection(collectionId.fullId(), cursor.toString())
                logger.info("Built filter: $filter")
                val queryResult = esItemRepository.search(filter, EsItemSort.DEFAULT, pageSize)
                logger.info("Query result: $queryResult")
                queryResult.entities.forEach { emit(IdParser.parseItemId(it.itemId)) }
                cursor = queryResult.continuation
                if (cursor == null) break
                returned += queryResult.entities.size
                check(returned < 1_000_000) { "Cyclic continuation $cursor for collection $collectionId" }
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

        val filter = itemFilterConverter.getItemsByCreator(creatorAddress.fullId(), continuation)
        logger.info("Built filter: $filter")
        val queryResult = esItemRepository.search(filter, EsItemSort.DEFAULT, safeSize)
        val cursor = queryResult.continuation
        logger.info("Query result: $queryResult")

        if (queryResult.entities.isEmpty()) return ItemsDto()
        val items = getItems(queryResult.entities)
        val enriched = itemEnrichService.enrich(items)

        return ItemsDto(
            items = enriched,
            continuation = cursor
        )
    }

    override suspend fun getItemsByOwner(
        owner: String,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ItemsDto {
        val evaluatedBlockchains = router.getEnabledBlockchains(blockchains)
        val ownerAddress = IdParser.parseAddress(owner)
        val ownerships = esOwnershipRepository.search(
            EsOwnershipByOwnerFilter(
                owner = ownerAddress,
                blockchains = evaluatedBlockchains,
                cursor = continuation,
            ),
            size,
        )

        val cursor = ownerships.lastOrNull()?.let { DateIdContinuation(it.date, it.ownershipId).toString() }
        val items: List<UnionItem> = getItemsByOwnerships(ownerships)

        val enriched = itemEnrichService.enrich(items)
        return ItemsDto(
            items = enriched,
            continuation = cursor
        )
    }

    override suspend fun getItemsByOwnerWithOwnership(
        owner: String,
        continuation: String?,
        size: Int?
    ): ItemsWithOwnershipDto {
        val ownerAddress = IdParser.parseAddress(owner)
        val safeSize = PageSize.OWNERSHIP.limit(size)
        val ownerships = ownershipElasticHelper.getRawOwnershipsByOwner(
            owner = ownerAddress,
            continuation = continuation,
            size = safeSize,
        )
        val resultOwnerships = ownerships.map { ItemOwnershipConverter.convert(it) }
        val cursor = ownerships.lastOrNull()?.let { DateIdContinuation(it.createdAt, it.id.fullId()).toString() }

        val items: List<UnionItem> = getItemsByIdsInner(resultOwnerships.map { it.id.getItemId().fullId() })
        val enriched = itemEnrichService.enrich(items)

        val result = resultOwnerships.mapNotNull { ownership ->
            val item = enriched.firstOrNull { it.id.fullId() == ownership.id.getItemId().fullId() } ?: return@mapNotNull null

            ItemWithOwnershipDto(
                item = item,
                ownership = ownership
            )
        }

        return ItemsWithOwnershipDto(
            items = result,
            continuation = cursor
        )
    }

    override suspend fun getItemsByIds(ids: List<ItemIdDto>): List<ItemDto> {
        throw NotImplementedError()
    }

    suspend fun searchItems(request: ItemsSearchRequestDto): ItemsDto {
        val cursor = DateIdContinuation.parse(request.continuation)?.toString()
        val filter = itemFilterConverter.searchItems(request.filter, cursor)
        val result = esItemRepository.search(filter, EsItemSort.DEFAULT, request.size)
        if (result.entities.isEmpty()) return ItemsDto()
        val items = getItems(result.entities)
        val enriched = itemEnrichService.enrich(items)
        return ItemsDto(
            continuation = result.continuation,
            items = enriched
        )
    }


    private suspend fun getItems(esItems: List<EsItem>): List<UnionItem> {
        val mapping = hashMapOf<BlockchainDto, MutableList<String>>()

        esItems.forEach { item ->
            mapping
                .computeIfAbsent(item.blockchain) { ArrayList(esItems.size) }
                .add(IdParser.parseItemId(item.itemId).value)
        }

        val items = getItemsFromBlockchains(mapping)
        val itemsIdMapping = items.associateBy { it.id.fullId() }

        return esItems.mapNotNull { esItem ->
            itemsIdMapping[esItem.itemId]
        }
    }

    private suspend fun getItemsByOwnerships(ownerships: List<EsOwnership>): List<UnionItem> {
        return getItemsByIdsInner(ownerships.mapNotNull { it.itemId })
    }

    private suspend fun getItemsByIdsInner(itemIds: List<String>): List<UnionItem> {
        val mapping = hashMapOf<BlockchainDto, MutableList<String>>()

        itemIds
            .forEach { itemId ->
                val itemIdDto = IdParser.parseItemId(itemId)
                mapping
                    .computeIfAbsent(itemIdDto.blockchain) { ArrayList(itemIds.size) }
                    .add(itemIdDto.value)
            }

        val items = getItemsFromBlockchains(mapping)
        val itemsIdMapping = items.associateBy { it.id.fullId() }

        return itemIds.mapNotNull {
            itemsIdMapping[it]
        }
    }

    private suspend fun getAllItemsInner(
        blockchains: List<BlockchainDto>?,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?,
        continuation: String?,
        size: Int?
    ): Slice<UnionItem> {
        logger.info("getAllActivities() from ElasticSearch")
        val evaluatedBlockchains = router.getEnabledBlockchains(blockchains).map { it.name }.toSet()

        val filter = itemFilterConverter.convertGetAllItems(
            evaluatedBlockchains, showDeleted, lastUpdatedFrom, lastUpdatedTo, continuation
        )
        logger.info("Built filter: $filter")
        val queryResult = esItemRepository.search(filter, EsItemSort.DEFAULT, size)
        logger.info("Query result: $queryResult")
        val items = getItems(queryResult.entities)
        return Slice(
            entities = items,
            continuation = queryResult.continuation
        )
    }

    private suspend fun getItemsFromBlockchains(itemsPerBlockchain: Map<BlockchainDto, MutableList<String>>): List<UnionItem> {
        logger.debug("Getting items from blockchains: $itemsPerBlockchain")
        val items = itemsPerBlockchain.mapAsync { element ->
            val blockchain = element.key
            val ids = element.value
            val isBlockchainEnabled = router.isBlockchainEnabled(blockchain)
            if (isBlockchainEnabled) {
                router.getService(blockchain).getItemsByIds(ids)
                    .also { logger.info("returned ${it.size} of ${ids.size} items from $blockchain") }
            } else emptyList()
        }.flatten()

        return items
    }
}
