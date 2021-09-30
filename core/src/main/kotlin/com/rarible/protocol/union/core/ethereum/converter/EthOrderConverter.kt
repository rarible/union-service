package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.CryptoPunkOrderDto
import com.rarible.protocol.dto.LegacyOrderDto
import com.rarible.protocol.dto.OpenSeaV1OrderDto
import com.rarible.protocol.dto.OrderCancelDto
import com.rarible.protocol.dto.OrderExchangeHistoryDto
import com.rarible.protocol.dto.OrderOpenSeaV1DataV1Dto
import com.rarible.protocol.dto.OrderSideDto
import com.rarible.protocol.dto.OrderSideMatchDto
import com.rarible.protocol.dto.OrdersPaginationDto
import com.rarible.protocol.dto.RaribleV2OrderDto
import com.rarible.protocol.union.core.continuation.Slice
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthOrderCryptoPunksDataDto
import com.rarible.protocol.union.dto.EthOrderDataLegacyDto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV1Dto
import com.rarible.protocol.union.dto.EthOrderOpenSeaV1DataV1Dto
import com.rarible.protocol.union.dto.OnChainOrderDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderPriceHistoryRecordDto
import com.rarible.protocol.union.dto.PendingOrderCancelDto
import com.rarible.protocol.union.dto.PendingOrderDto
import com.rarible.protocol.union.dto.PendingOrderMatchDto
import com.rarible.protocol.union.dto.PlatformDto
import java.time.Instant

object EthOrderConverter {

    fun convert(order: com.rarible.protocol.dto.OrderDto, blockchain: BlockchainDto): OrderDto {
        val orderId = OrderIdDto(blockchain, EthConverter.convert(order.hash))
        return when (order) {
            is LegacyOrderDto -> {
                OrderDto(
                    id = orderId,
                    platform = PlatformDto.RARIBLE,
                    maker = UnionAddressConverter.convert(order.maker, blockchain),
                    taker = order.taker?.let { UnionAddressConverter.convert(it, blockchain) },
                    make = EthConverter.convert(order.make, blockchain),
                    take = EthConverter.convert(order.take, blockchain),
                    salt = EthConverter.convert(order.salt),
                    signature = order.signature?.let { EthConverter.convert(it) },
                    pending = order.pending?.map { convert(it, blockchain) },
                    fill = order.fillValue!!,
                    startedAt = order.start?.let { Instant.ofEpochSecond(it) },
                    endedAt = order.end?.let { Instant.ofEpochSecond(it) },
                    makeStock = order.makeStockValue!!,
                    cancelled = order.cancelled,
                    createdAt = order.createdAt,
                    lastUpdatedAt = order.lastUpdateAt,
                    makePriceUsd = order.makePriceUsd,
                    takePriceUsd = order.takePriceUsd,
                    priceHistory = order.priceHistory?.map { convert(it) } ?: listOf(),
                    data = EthOrderDataLegacyDto(
                        fee = order.data.fee.toBigInteger()
                    )
                )
            }
            is RaribleV2OrderDto -> {
                OrderDto(
                    id = orderId,
                    platform = PlatformDto.RARIBLE,
                    maker = UnionAddressConverter.convert(order.maker, blockchain),
                    taker = order.taker?.let { UnionAddressConverter.convert(order.taker!!, blockchain) },
                    make = EthConverter.convert(order.make, blockchain),
                    take = EthConverter.convert(order.take, blockchain),
                    salt = EthConverter.convert(order.salt),
                    signature = order.signature?.let { EthConverter.convert(it) },
                    pending = order.pending?.map { convert(it, blockchain) },
                    fill = order.fillValue!!,
                    startedAt = order.start?.let { Instant.ofEpochSecond(it) },
                    endedAt = order.end?.let { Instant.ofEpochSecond(it) },
                    makeStock = order.makeStockValue!!,
                    cancelled = order.cancelled,
                    createdAt = order.createdAt,
                    lastUpdatedAt = order.lastUpdateAt,
                    makePriceUsd = order.makePriceUsd,
                    takePriceUsd = order.takePriceUsd,
                    priceHistory = order.priceHistory?.map { convert(it) } ?: listOf(),
                    data = EthOrderDataRaribleV2DataV1Dto(
                        payouts = order.data.payouts.map { EthConverter.convertToPayout(it, blockchain) },
                        originFees = order.data.originFees.map { EthConverter.convertToPayout(it, blockchain) }
                    )
                )
            }
            is OpenSeaV1OrderDto -> {
                OrderDto(
                    id = orderId,
                    platform = PlatformDto.OPEN_SEA,
                    maker = UnionAddressConverter.convert(order.maker, blockchain),
                    taker = order.taker?.let { UnionAddressConverter.convert(order.taker!!, blockchain) },
                    make = EthConverter.convert(order.make, blockchain),
                    take = EthConverter.convert(order.take, blockchain),
                    salt = EthConverter.convert(order.salt),
                    signature = order.signature?.let { EthConverter.convert(it) },
                    pending = order.pending?.map { convert(it, blockchain) },
                    fill = order.fillValue!!,
                    startedAt = order.start?.let { Instant.ofEpochSecond(it) },
                    endedAt = order.end?.let { Instant.ofEpochSecond(it) },
                    makeStock = order.makeStockValue!!,
                    cancelled = order.cancelled,
                    createdAt = order.createdAt,
                    lastUpdatedAt = order.lastUpdateAt,
                    makePriceUsd = order.makePriceUsd,
                    takePriceUsd = order.takePriceUsd,
                    priceHistory = order.priceHistory?.map { convert(it) } ?: listOf(),
                    data = EthOrderOpenSeaV1DataV1Dto(
                        exchange = UnionAddressConverter.convert(order.data.exchange, blockchain),
                        makerRelayerFee = order.data.makerRelayerFee,
                        takerRelayerFee = order.data.takerRelayerFee,
                        makerProtocolFee = order.data.makerProtocolFee,
                        takerProtocolFee = order.data.takerProtocolFee,
                        feeRecipient = UnionAddressConverter.convert(order.data.feeRecipient, blockchain),
                        feeMethod = convert(order.data.feeMethod),
                        side = convert(order.data.side),
                        saleKind = convert(order.data.saleKind),
                        howToCall = convert(order.data.howToCall),
                        callData = EthConverter.convert(order.data.callData),
                        replacementPattern = EthConverter.convert(order.data.replacementPattern),
                        staticTarget = UnionAddressConverter.convert(order.data.staticTarget, blockchain),
                        staticExtraData = EthConverter.convert(order.data.staticExtraData),
                        extra = order.data.extra
                    )
                )
            }
            is CryptoPunkOrderDto -> {
                OrderDto(
                    id = orderId,
                    platform = PlatformDto.CRYPTO_PUNKS,
                    maker = UnionAddressConverter.convert(order.maker, blockchain),
                    taker = order.taker?.let { UnionAddressConverter.convert(order.taker!!, blockchain) },
                    make = EthConverter.convert(order.make, blockchain),
                    take = EthConverter.convert(order.take, blockchain),
                    salt = EthConverter.convert(order.salt),
                    signature = order.signature?.let { EthConverter.convert(it) },
                    pending = order.pending?.map { convert(it, blockchain) },
                    fill = order.fillValue!!,
                    startedAt = order.start?.let { Instant.ofEpochSecond(it) },
                    endedAt = order.end?.let { Instant.ofEpochSecond(it) },
                    makeStock = order.makeStockValue!!,
                    cancelled = order.cancelled,
                    createdAt = order.createdAt,
                    lastUpdatedAt = order.lastUpdateAt,
                    makePriceUsd = order.makePriceUsd,
                    takePriceUsd = order.takePriceUsd,
                    priceHistory = order.priceHistory?.map { convert(it) } ?: listOf(),
                    data = EthOrderCryptoPunksDataDto()
                )
            }
        }
    }

    fun convert(source: OrdersPaginationDto, blockchain: BlockchainDto): Slice<OrderDto> {
        return Slice(
            continuation = source.continuation,
            entities = source.orders.map { convert(it, blockchain) }
        )
    }

    private fun convert(source: OrderOpenSeaV1DataV1Dto.FeeMethod): EthOrderOpenSeaV1DataV1Dto.FeeMethod {
        return when (source) {
            OrderOpenSeaV1DataV1Dto.FeeMethod.PROTOCOL_FEE -> EthOrderOpenSeaV1DataV1Dto.FeeMethod.PROTOCOL_FEE
            OrderOpenSeaV1DataV1Dto.FeeMethod.SPLIT_FEE -> EthOrderOpenSeaV1DataV1Dto.FeeMethod.SPLIT_FEE
        }
    }

    private fun convert(source: OrderOpenSeaV1DataV1Dto.Side): EthOrderOpenSeaV1DataV1Dto.Side {
        return when (source) {
            OrderOpenSeaV1DataV1Dto.Side.SELL -> EthOrderOpenSeaV1DataV1Dto.Side.SELL
            OrderOpenSeaV1DataV1Dto.Side.BUY -> EthOrderOpenSeaV1DataV1Dto.Side.BUY
        }
    }

    private fun convert(source: OrderOpenSeaV1DataV1Dto.SaleKind): EthOrderOpenSeaV1DataV1Dto.SaleKind {
        return when (source) {
            OrderOpenSeaV1DataV1Dto.SaleKind.FIXED_PRICE -> EthOrderOpenSeaV1DataV1Dto.SaleKind.FIXED_PRICE
            OrderOpenSeaV1DataV1Dto.SaleKind.DUTCH_AUCTION -> EthOrderOpenSeaV1DataV1Dto.SaleKind.DUTCH_AUCTION
        }
    }

    private fun convert(source: OrderOpenSeaV1DataV1Dto.HowToCall): EthOrderOpenSeaV1DataV1Dto.HowToCall {
        return when (source) {
            OrderOpenSeaV1DataV1Dto.HowToCall.CALL -> EthOrderOpenSeaV1DataV1Dto.HowToCall.CALL
            OrderOpenSeaV1DataV1Dto.HowToCall.DELEGATE_CALL -> EthOrderOpenSeaV1DataV1Dto.HowToCall.DELEGATE_CALL
        }
    }


    private fun convert(source: OrderSideDto): PendingOrderMatchDto.Side {
        return when (source) {
            OrderSideDto.RIGHT -> PendingOrderMatchDto.Side.RIGHT
            OrderSideDto.LEFT -> PendingOrderMatchDto.Side.LEFT
        }
    }

    private fun convert(source: com.rarible.protocol.dto.OrderPriceHistoryRecordDto): OrderPriceHistoryRecordDto {
        return OrderPriceHistoryRecordDto(
            date = source.date,
            makeValue = source.makeValue,
            takeValue = source.takeValue
        )
    }

    private fun convert(source: OrderExchangeHistoryDto, blockchain: BlockchainDto): PendingOrderDto {
        return when (source) {
            is OrderSideMatchDto -> PendingOrderMatchDto(
                id = OrderIdDto(blockchain, EthConverter.convert(source.hash)),
                make = source.make?.let { EthConverter.convert(it, blockchain) },
                take = source.take?.let { EthConverter.convert(it, blockchain) },
                date = source.date,
                side = source.side?.let { convert(it) },
                /** TODO [OrderSideMatchDto.fill] must be BigDecimal, or fillValue */
                fill = source.fill.toBigDecimal(),
                maker = source.maker?.let { UnionAddressConverter.convert(it, blockchain) },
                taker = source.taker?.let { UnionAddressConverter.convert(it, blockchain) },
                counterHash = source.counterHash?.let { EthConverter.convert(it) },
                makeUsd = source.makeUsd,
                takeUsd = source.takeUsd,
                makePriceUsd = source.makePriceUsd,
                takePriceUsd = source.takePriceUsd
            )
            is OrderCancelDto -> PendingOrderCancelDto(
                id = OrderIdDto(blockchain, EthConverter.convert(source.hash)),
                make = source.make?.let { EthConverter.convert(it, blockchain) },
                take = source.take?.let { EthConverter.convert(it, blockchain) },
                date = source.date,
                maker = source.maker?.let { UnionAddressConverter.convert(it, blockchain) },
                owner = source.owner?.let { UnionAddressConverter.convert(it, blockchain) }
            )
            is com.rarible.protocol.dto.OnChainOrderDto -> OnChainOrderDto(
                id = OrderIdDto(blockchain, EthConverter.convert(source.hash)),
                make = source.make?.let { EthConverter.convert(it, blockchain) },
                take = source.take?.let { EthConverter.convert(it, blockchain) },
                date = source.date,
                maker = source.maker?.let { UnionAddressConverter.convert(it, blockchain) }
            )
        }
    }

}

