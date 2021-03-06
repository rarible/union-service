package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.logging.Logger
import com.rarible.protocol.tezos.dto.AssetDto
import com.rarible.protocol.tezos.dto.AssetTypeDto
import com.rarible.protocol.tezos.dto.BurnDto
import com.rarible.protocol.tezos.dto.FTAssetTypeDto
import com.rarible.protocol.tezos.dto.MTAssetTypeDto
import com.rarible.protocol.tezos.dto.MintDto
import com.rarible.protocol.tezos.dto.NFTAssetTypeDto
import com.rarible.protocol.tezos.dto.NftActTypeDto
import com.rarible.protocol.tezos.dto.NftActivitiesDto
import com.rarible.protocol.tezos.dto.NftActivityEltDto
import com.rarible.protocol.tezos.dto.OrderActTypeDto
import com.rarible.protocol.tezos.dto.OrderActivitiesDto
import com.rarible.protocol.tezos.dto.OrderActivityCancelListDto
import com.rarible.protocol.tezos.dto.OrderActivityListDto
import com.rarible.protocol.tezos.dto.OrderActivityMatchDto
import com.rarible.protocol.tezos.dto.OrderActivityMatchTypeDto
import com.rarible.protocol.tezos.dto.OrderActivitySideMatchDto
import com.rarible.protocol.tezos.dto.OrderActivitySideTypeDto
import com.rarible.protocol.tezos.dto.TransferDto
import com.rarible.protocol.tezos.dto.XTZAssetTypeDto
import com.rarible.protocol.union.dto.PlatformDto
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.Row
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.time.Instant

class TezosPgActivityService(
    connectionFactory: ConnectionFactory
) {

    val client = DatabaseClient.create(connectionFactory)

    suspend fun orderActivities(ids: List<String>): OrderActivitiesDto = coroutineScope {
        val listAndCancelActivities = async { orderListAndCancelActivities(ids) }
        val matchActivities = async { orderMatchActivities(ids) }

        OrderActivitiesDto(
            items = listAndCancelActivities.await() + matchActivities.await()
        )
    }

    suspend fun nftActivities(ids: List<String>): NftActivitiesDto {
        val selectOrders = """
            select tmp.* from 
                (select concat(block, '_', index) AS activity_id, * from nft_activities where block in (:blocks)) as tmp
            where tmp.activity_id in (:ids)
        """
        val blocks = ids.map { it.split("_").first() }

        val activities = client.sql(selectOrders)
            .bind("blocks", blocks)
            .bind("ids", ids)
            .map(this::convertNft)
            .all()
            .collectList().awaitSingle()

        return NftActivitiesDto(
            items = activities
        )
    }

    private suspend fun orderListAndCancelActivities(ids: List<String>): List<OrderActTypeDto> {
        val selectOrders = """
            select o.*, a.order_activity_type, a.id, a.date from order_activities a
            left join orders o on o.hash = a.hash
            where a.id in (:ids) and a.order_activity_type in ('list', 'cancel_l')
        """

        return client.sql(selectOrders)
            .bind("ids", ids)
            .map(this::saveConvertList)
            .all()
            .collectList()
            .awaitSingle().filterNotNull()
    }

    private suspend fun orderMatchActivities(ids: List<String>): List<OrderActTypeDto> {
        val selectOrders = """
            select
                l.hash l_hash, l.maker, r.hash r_hash, r.maker taker,
            
                l.make_asset_type_class l_make_asset_type_class,
                l.make_asset_type_contract l_make_asset_type_contract,
                l.make_asset_type_token_id l_make_asset_type_token_id,
                l.make_asset_value l_make_asset_value,
                l.make_asset_decimals l_make_asset_decimals,
            
                l.take_asset_type_class l_take_asset_type_class,
                l.take_asset_type_contract l_take_asset_type_contract,
                l.take_asset_type_token_id l_take_asset_type_token_id,
                l.take_asset_value l_take_asset_value,
                l.take_asset_decimals l_take_asset_decimals,
                l.salt l_salt,
            
                r.make_asset_type_class r_make_asset_type_class,
                r.make_asset_type_contract r_make_asset_type_contract,
                r.make_asset_type_token_id r_make_asset_type_token_id,
                r.make_asset_value r_make_asset_value,
                r.make_asset_decimals r_make_asset_decimals,
            
                r.take_asset_type_class r_take_asset_type_class,
                r.take_asset_type_contract r_take_asset_type_contract,
                r.take_asset_type_token_id r_take_asset_type_token_id,
                r.take_asset_value r_take_asset_value,
                r.take_asset_decimals r_take_asset_decimals,
                r.salt r_salt,
            
                a.id, a.date from order_activities a
                     left join orders l on l.hash = a.match_left
                     left join orders r on r.hash = a.match_right
            where a.id in (:ids) and a.order_activity_type = 'match'
        """

        return client.sql(selectOrders)
            .bind("ids", ids).map(this::convertMatch)
            .all()
            .collectList().awaitSingle()
    }

    private fun saveConvertList(row: Row): OrderActTypeDto? {
        return try {
            logger.error("Failed to convert row: ${row}")
            convertList(row)
        } catch (ex: Exception) {
            logger.error("Failed to convert row: ${row}", ex)
            null
        }
    }

    private fun convertList(row: Row): OrderActTypeDto {
        val id = row.get("id", String::class.java)
        val hash = row.get("hash", String::class.java)
        val makeAssetType = assetType(TYPE.MAKE, row)
        val makeValue = price(TYPE.MAKE, row)
        val takeAssetValue = assetType(TYPE.TAKE, row)
        val takeValue = price(TYPE.TAKE, row)
        val price = price(makeAssetType, makeValue, takeValue)
        val subType = when (ACTIVITY_TYPE.get(row.get("order_activity_type", String::class.java))) {
            ACTIVITY_TYPE.LIST -> OrderActivityListDto(
                price = price,
                hash = hash,
                maker = row.get("maker", String::class.java),
                make = AssetDto(
                    assetType = makeAssetType,
                    value = makeValue
                ),
                take = AssetDto(
                    assetType = takeAssetValue,
                    value = takeValue
                ),
            )
            ACTIVITY_TYPE.CANCEL_LIST -> OrderActivityCancelListDto(
                hash = hash,
                maker = row.get("maker", String::class.java),
                make = makeAssetType,
                take = takeAssetValue,

                // deprecated
                transactionHash = hash,
                blockHash = "",
                blockNumber = BigInteger.ZERO,
                logIndex = 0
            )
        }
        return OrderActTypeDto(
            id = id,
            date = row.get("date", Instant::class.java),
            source = PlatformDto.RARIBLE.toString(),
            type = subType
        )
    }

    private fun convertMatch(row: Row): OrderActTypeDto {
        val id = row.get("id", String::class.java)
        val leftAssetType = assetType("l_make", row)
        val rightAssetType = assetType("r_make", row)
        val rightMakeValue = price("r_make", row)
        val rightTakeValue = price("r_take", row)
        val price = price(rightAssetType, rightMakeValue, rightTakeValue)
        return OrderActTypeDto(
            id = id,
            date = row.get("date", Instant::class.java),
            source = PlatformDto.RARIBLE.toString(),
            type = OrderActivityMatchDto(
                type = OrderActivityMatchTypeDto.SELL,
                price = price,
                left = OrderActivitySideMatchDto(
                    maker = row.get("maker", String::class.java),
                    hash = row.get("l_hash", String::class.java),
                    asset = AssetDto(
                        assetType = leftAssetType,
                        value = rightTakeValue
                    ),
                    type = activityType(leftAssetType)
                ),
                right = OrderActivitySideMatchDto(
                    maker = row.get("taker", String::class.java),
                    hash = row.get("r_hash", String::class.java),
                    asset = AssetDto(
                        assetType = rightAssetType,
                        value = rightMakeValue
                    ),
                    type = activityType(rightAssetType)
                ),
                // deprecated
                transactionHash = "",
                blockHash = "",
                blockNumber = BigInteger.ZERO,
                logIndex = 0
            )
        )
    }

    private fun convertNft(row: Row): NftActTypeDto {
        val id = row.get("activity_id", String::class.java)
        val subType = when (NFT_ACTIVITY_TYPE.get(row.get("activity_type", String::class.java))) {
            NFT_ACTIVITY_TYPE.MINT -> MintDto(
                owner = row.get("owner", String::class.java),
                contract = row.get("contract", String::class.java),
                tokenId = BigInteger(row.get("token_id", String::class.java)),
                value = row.get("amount", BigDecimal::class.java),
                transactionHash = row.get("transaction", String::class.java),

                // deprecated
                blockHash = "",
                blockNumber = BigInteger.ZERO
            )
            NFT_ACTIVITY_TYPE.TRANSFER -> TransferDto(
                elt = NftActivityEltDto(
                    owner = row.get("owner", String::class.java),
                    contract = row.get("contract", String::class.java),
                    tokenId = BigInteger(row.get("token_id", String::class.java)),
                    value = row.get("amount", BigDecimal::class.java),
                    transactionHash = row.get("transaction", String::class.java),

                    // deprecated
                    blockHash = "",
                    blockNumber = BigInteger.ZERO
                ),
                from = row.get("tr_from", String::class.java)
            )
            NFT_ACTIVITY_TYPE.BURN -> BurnDto(
                owner = row.get("owner", String::class.java),
                contract = row.get("contract", String::class.java),
                tokenId = BigInteger(row.get("token_id", String::class.java)),
                value = row.get("amount", BigDecimal::class.java),
                transactionHash = row.get("transaction", String::class.java),

                // deprecated
                blockHash = "",
                blockNumber = BigInteger.ZERO
            )
        }
        return NftActTypeDto(
            id = id,
            date = row.get("date", Instant::class.java),
            source = PlatformDto.RARIBLE.toString(),
            type = subType
        )
    }

    private fun activityType(assetType: AssetTypeDto): OrderActivitySideTypeDto = when (assetType) {
        is XTZAssetTypeDto -> OrderActivitySideTypeDto.SELL
        else -> OrderActivitySideTypeDto.BID
    }

    private fun price(assetType: AssetTypeDto, make: BigDecimal, take: BigDecimal) = when (assetType) {
        is XTZAssetTypeDto -> make.divide(take, MathContext.DECIMAL128)
        else -> take.divide(make, MathContext.DECIMAL128)
    }

    private fun price(type: TYPE, row: Row) = price(type.value, row)

    private fun price(type: String, row: Row): BigDecimal {
        val makeDecimals = row.get("${type}_asset_decimals")?.let { Integer.parseInt(it.toString()) } ?: 0
        return row.get("${type}_asset_value", BigDecimal::class.java)
            .divide(BigDecimal("10").pow(makeDecimals))
    }

    private fun assetType(type: TYPE, row: Row) = assetType(type.value, row)

    private fun assetType(type: String, row: Row): AssetTypeDto {
        val typeTxt = row.get("${type}_asset_type_class", String::class.java)
        val contract = row.get("${type}_asset_type_contract", String::class.java)
        val tokenId = row.get("${type}_asset_type_token_id", String::class.java)?.let { BigInteger(it) }
        return when (typeTxt) {
            "MT" -> MTAssetTypeDto(contract!!, tokenId!!)
            "XTZ" -> XTZAssetTypeDto()
            "NFT" -> NFTAssetTypeDto(contract!!, tokenId!!)
            "FT" -> FTAssetTypeDto(contract!!, tokenId)
            else -> throw RuntimeException("Unknown type: $type")
        }
    }

    companion object {
        private val logger by Logger()
    }

    enum class TYPE(val value: String) {
        MAKE("make"),
        TAKE("take")
    }

    enum class ACTIVITY_TYPE(val value: String) {
        LIST("list"),
        CANCEL_LIST("cancel_l");

        companion object {
            fun get(value: String) = values().firstOrNull { it.value.equals(value) }
                ?: throw RuntimeException("ActivityType $value is unknown")
        }
    }

    enum class NFT_ACTIVITY_TYPE(val value: String) {
        MINT("mint"),
        TRANSFER("transfer"),
        BURN("burn");

        companion object {
            fun get(value: String) = values().firstOrNull { it.value.equals(value) }
                ?: throw RuntimeException("NftActivityType $value is unknown")
        }
    }
}
