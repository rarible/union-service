package com.rarible.protocol.union.integration.tezos.dipdup.converter

import com.rarible.dipdup.client.core.model.Asset
import com.rarible.dipdup.client.core.model.DipDupOrder
import com.rarible.dipdup.client.core.model.OrderStatus
import com.rarible.dipdup.client.core.model.Part
import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.dipdup.client.model.DipDupOrderSort
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.core.util.evalMakePrice
import com.rarible.protocol.union.core.util.evalTakePrice
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDataDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PayoutDto
import com.rarible.protocol.union.dto.TezosOrderDataRaribleV2DataV1Dto
import com.rarible.protocol.union.dto.TezosOrderDataRaribleV2DataV2Dto
import com.rarible.protocol.union.dto.ext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class DipDupOrderConverter(
    private val currencyService: CurrencyService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun convert(order: DipDupOrder, blockchain: BlockchainDto): OrderDto {
        try {
            return convertInternal(order, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Order: {} \n{}", blockchain, e.message, order)
            throw e
        }
    }

    suspend fun convert(source: List<Asset.AssetType>, blockchain: BlockchainDto): List<AssetTypeDto> {
        try {
            return source.map { DipDupConverter.convert(it, blockchain) }
        } catch (e: Exception) {
            logger.error("Failed to convert {} list of assets: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    private suspend fun convertInternal(order: DipDupOrder, blockchain: BlockchainDto): OrderDto {

        var make = DipDupConverter.convert(order.make, blockchain)
        var take = DipDupConverter.convert(order.take, blockchain)

        // It's a critical bug in dupdup-indexer: there's a price for 1 item in take instead of full price of order
        // Need to fix in the indexer lately
        if (order.platform == TezosPlatform.RARIBLE_V2) {
            if (make.type.ext.isNft) {
                take = take.copy(value = take.value * make.value)
            }
            if (take.type.ext.isNft) {
                make = make.copy(value = make.value * take.value)
            }
        }

        val maker = UnionAddressConverter.convert(blockchain, order.maker)
        val taker = order.taker?.let { UnionAddressConverter.convert(blockchain, it) }

        // For BID (make = currency, take - NFT) we're calculating prices for taker
        val takePrice = evalTakePrice(make, take)
        // For SELL (make = NFT, take - currency) we're calculating prices for maker
        val makePrice = evalMakePrice(make, take)

        // So for USD conversion we are using take.type for MAKE price and vice versa
        val makePriceUsd = currencyService.toUsd(blockchain, take.type, makePrice)
        val takePriceUsd = currencyService.toUsd(blockchain, make.type, takePrice)

        val status = convert(order.status)

        return OrderDto(
            id = OrderIdDto(blockchain, order.id),
            platform = DipDupConverter.convert(order.platform),
            maker = maker,
            taker = taker,
            make = make,
            take = take,
            status = status,
            fill = order.fill,
            startedAt = order.startAt?.toInstant(),
            endedAt = order.endAt?.toInstant(),
            makeStock = makeStock(order.make, order.fill),
            cancelled = order.cancelled,
            createdAt = order.createdAt.toInstant(),
            lastUpdatedAt = order.lastUpdatedAt.toInstant(),
            makePrice = makePrice,
            takePrice = takePrice,
            makePriceUsd = makePriceUsd,
            takePriceUsd = takePriceUsd,
            data = orderData(order, blockchain),
            salt = order.salt.toString(),
            pending = emptyList()
        )
    }

    fun makeStock(asset: Asset, fill: BigDecimal): BigDecimal {
        return asset.assetValue - fill
    }

    fun orderData(order: DipDupOrder, blockchain: BlockchainDto): OrderDataDto {
        return when (order.platform) {
            TezosPlatform.RARIBLE_V2 -> TezosOrderDataRaribleV2DataV2Dto(
                payouts = order.payouts.map { convert(it, blockchain) },
                originFees = order.originFees.map { convert(it, blockchain) }
            )
            else -> TezosOrderDataRaribleV2DataV1Dto(
                payouts = order.payouts.map { convert(it, blockchain) },
                originFees = order.originFees.map { convert(it, blockchain) }
            )
        }
    }

    private fun convert(source: Part, blockchain: BlockchainDto): PayoutDto {
        return PayoutDto(
            account = UnionAddressConverter.convert(blockchain, source.account),
            value = source.value
        )
    }

    fun convert(source: OrderStatus): OrderStatusDto {
        return when (source) {
            OrderStatus.ACTIVE -> OrderStatusDto.ACTIVE
            OrderStatus.FILLED -> OrderStatusDto.FILLED
            OrderStatus.CANCELLED -> OrderStatusDto.CANCELLED
            OrderStatus.INACTIVE -> OrderStatusDto.INACTIVE
            OrderStatus.HISTORICAL -> OrderStatusDto.HISTORICAL
        }
    }

    fun convert(source: OrderStatusDto): OrderStatus {
        return when (source) {
            OrderStatusDto.ACTIVE -> OrderStatus.ACTIVE
            OrderStatusDto.FILLED -> OrderStatus.FILLED
            OrderStatusDto.CANCELLED -> OrderStatus.CANCELLED
            OrderStatusDto.INACTIVE -> OrderStatus.INACTIVE
            OrderStatusDto.HISTORICAL -> OrderStatus.HISTORICAL
        }
    }

    fun convert(source: OrderSortDto) = when (source) {
        OrderSortDto.LAST_UPDATE_ASC -> DipDupOrderSort.LAST_UPDATE_ASC
        OrderSortDto.LAST_UPDATE_DESC -> DipDupOrderSort.LAST_UPDATE_DESC
    }
}
