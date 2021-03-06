import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBoolean
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.AuctionBidDto
import com.rarible.protocol.union.dto.AuctionDataDto
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionHistoryDto
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.AuctionStatusDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV1Dto
import com.rarible.protocol.union.dto.ItemHistoryDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderDataDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.PendingOrderDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.RaribleAuctionV1DataV1Dto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.group
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.temporal.ChronoUnit

fun randomOwnershipId(
    blockchain: BlockchainDto = BlockchainDto.values().random(),
    itemIdValue: String = "${randomString()}:${randomLong()}",
    owner: UnionAddress = randomUnionAddress(blockchain, randomString()),
) = OwnershipIdDto(
    blockchain = blockchain,
    itemIdValue = itemIdValue,
    owner = owner,
)

fun randomOwnership(
    id: OwnershipIdDto = randomOwnershipId(),
    blockchain: BlockchainDto = id.blockchain,
    itemId: ItemIdDto? = ItemIdDto(id.blockchain, id.itemIdValue),
    contract: ContractAddress? = ContractAddress(id.blockchain, randomString()),
    collection: CollectionIdDto? = CollectionIdDto(id.blockchain, randomString()),
    tokenId: BigInteger? = randomBigInt(),
    owner: UnionAddress = UnionAddress(id.blockchain.group(), randomString()),
    value: BigInteger = randomBigInt(),
    createdAt: Instant = Instant.ofEpochMilli(randomLong()),
    creators: List<CreatorDto>? = listOf(CreatorDto(randomUnionAddress(id.blockchain), randomInt())),
    lazyValue: BigInteger = randomBigInt(),
    pending: List<ItemHistoryDto> = listOf(),
    auction: AuctionDto? = randomAuction(id = randomAuctionId(id.blockchain)),
    bestSellOrder: OrderDto? = randomOrder(id = randomOrderId(id.blockchain)),
) = OwnershipDto(
    id = id,
    blockchain = blockchain,
    itemId = itemId,
    contract = contract,
    collection = collection,
    tokenId = tokenId,
    owner = owner,
    value = value,
    createdAt = createdAt,
    creators = creators,
    lazyValue = lazyValue,
    pending = pending,
    auction = auction,
    bestSellOrder = bestSellOrder,
)

fun randomUnionAddress(
    blockchain: BlockchainDto = BlockchainDto.values().random(),
    value: String = randomString(),
) = UnionAddress(blockchain.group(), value)

fun randomAuctionId(
    blockchain: BlockchainDto = BlockchainDto.values().random(),
    value: String = randomString(),
) = AuctionIdDto(blockchain, value)

fun randomOrderId(
    blockchain: BlockchainDto = BlockchainDto.values().random(),
    value: String = randomString(),
) = OrderIdDto(blockchain, value)

fun randomInstant(): Instant = nowMillis().minusMillis(randomLong(14400000)).truncatedTo(ChronoUnit.MILLIS)

fun randomAuction(
    id: AuctionIdDto = randomAuctionId(),
    contract: ContractAddress = ContractAddress(id.blockchain, randomString()),
    type: AuctionDto.Type? = AuctionDto.Type.values().random(),
    seller: UnionAddress = randomUnionAddress(id.blockchain),
    sell: AssetDto = AssetDto(randomAssetType(id.blockchain), randomBigDecimal()),
    buy: AssetTypeDto = randomAssetType(id.blockchain),
    endTime: Instant? = randomInstant(),
    minimalStep: BigDecimal = randomBigDecimal(),
    minimalPrice: BigDecimal = randomBigDecimal(),
    createdAt: Instant = randomInstant(),
    lastUpdateAt: Instant = randomInstant(),
    buyPrice: BigDecimal? = randomBigDecimal(),
    buyPriceUsd: BigDecimal? = randomBigDecimal(),
    pending: List<AuctionHistoryDto>? = null,
    status: AuctionStatusDto = AuctionStatusDto.values().random(),
    ongoing: Boolean = randomBoolean(),
    hash: String = randomString(),
    auctionId: BigInteger = randomBigInt(),
    lastBid: AuctionBidDto? = null,
    data: AuctionDataDto = RaribleAuctionV1DataV1Dto(emptyList(), emptyList(), duration = randomBigInt()),
) = AuctionDto(
    id = id,
    contract = contract,
    type = type,
    seller = seller,
    sell = sell,
    buy = buy,
    endTime = endTime,
    minimalStep = minimalStep,
    minimalPrice = minimalPrice,
    createdAt = createdAt,
    lastUpdateAt = lastUpdateAt,
    buyPrice = buyPrice,
    buyPriceUsd = buyPriceUsd,
    pending = pending,
    status = status,
    ongoing = ongoing,
    hash = hash,
    auctionId = auctionId,
    lastBid = lastBid,
    data = data,
)

fun randomAssetType(blockchain: BlockchainDto): AssetTypeDto = EthErc721AssetTypeDto(
    contract = ContractAddress(blockchain, randomString()),
    tokenId = randomBigInt(),
)

fun randomOrder(
    id: OrderIdDto = randomOrderId(),
    fill: BigDecimal = randomBigDecimal(),
    platform: PlatformDto = PlatformDto.values().random(),
    status: OrderStatusDto = OrderStatusDto.values().random(),
    startedAt: Instant? = null,
    endedAt: Instant? = null,
    makeStock: BigDecimal = randomBigDecimal(),
    cancelled: Boolean = randomBoolean(),
    createdAt: Instant = randomInstant(),
    lastUpdatedAt: Instant = randomInstant(),
    makePrice: BigDecimal? = null,
    takePrice: BigDecimal? = null,
    makePriceUsd: BigDecimal? = null,
    takePriceUsd: BigDecimal? = null,
    maker: UnionAddress = randomUnionAddress(id.blockchain),
    taker: UnionAddress? = null,
    make: AssetDto = AssetDto(randomAssetType(id.blockchain), randomBigDecimal()),
    take: AssetDto = AssetDto(randomAssetType(id.blockchain), randomBigDecimal()),
    salt: String = randomString(),
    signature: String? = null,
    pending: List<PendingOrderDto>? = listOf(),
    data: OrderDataDto = randomOrderData(),
) = OrderDto(
    id = id,
    fill = fill,
    platform = platform,
    status = status,
    startedAt = startedAt,
    endedAt = endedAt,
    makeStock = makeStock,
    cancelled = cancelled,
    createdAt = createdAt,
    lastUpdatedAt = lastUpdatedAt,
    makePrice = makePrice,
    takePrice = takePrice,
    makePriceUsd = makePriceUsd,
    takePriceUsd = takePriceUsd,
    maker = maker,
    taker = taker,
    make = make,
    take = take,
    salt = salt,
    signature = signature,
    pending = pending,
    data = data,
)

fun randomOrderData(): OrderDataDto = EthOrderDataRaribleV2DataV1Dto(emptyList(), emptyList())

fun randomEsOwnership(
    id: OwnershipIdDto = randomOwnershipId(),
) = EsOwnership(
    ownershipId = id.fullId(),
    blockchain = id.blockchain,
    itemId = ItemIdDto(id.blockchain, id.itemIdValue).fullId(),
    collection = CollectionIdDto(id.blockchain, randomString()).fullId(),
    owner = id.owner.fullId(),
    date = randomInstant(),
    auctionId = randomString(),
    auctionOwnershipId = OwnershipIdDto(
        id.blockchain,
        id.itemIdValue,
        randomUnionAddress(id.blockchain, randomString())
    ).fullId(),
)
