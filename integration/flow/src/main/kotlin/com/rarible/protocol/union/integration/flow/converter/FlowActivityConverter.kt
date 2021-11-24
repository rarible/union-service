package com.rarible.protocol.union.integration.flow.converter

import com.rarible.protocol.dto.FlowActivitiesDto
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowAssetDto
import com.rarible.protocol.dto.FlowBurnDto
import com.rarible.protocol.dto.FlowMintDto
import com.rarible.protocol.dto.FlowNftOrderActivityCancelListDto
import com.rarible.protocol.dto.FlowNftOrderActivityListDto
import com.rarible.protocol.dto.FlowNftOrderActivitySellDto
import com.rarible.protocol.dto.FlowTransferDto
import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.ActivityBlockchainInfoDto
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import com.rarible.protocol.union.dto.OrderCancelListActivityDto
import com.rarible.protocol.union.dto.OrderListActivityDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.TransferActivityDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class FlowActivityConverter(
    private val currencyService: CurrencyService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun convert(source: FlowActivityDto, blockchain: BlockchainDto): ActivityDto {
        try {
            return convertInternal(source, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Activity: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    private suspend fun convertInternal(source: FlowActivityDto, blockchain: BlockchainDto): ActivityDto {
        val activityId = ActivityIdDto(blockchain, source.id)
        return when (source) {
            is FlowNftOrderActivitySellDto -> {
                val nft = FlowConverter.convert(source.left.asset, blockchain)
                val payment = FlowConverter.convert(source.right.asset, blockchain)
                val priceUsd = currencyService
                    .toUsd(blockchain, payment.type, source.price) ?: BigDecimal.ZERO

                OrderMatchSellDto(
                    id = activityId,
                    date = source.date,
                    nft = nft,
                    payment = payment,
                    seller = UnionAddressConverter.convert(blockchain, source.left.maker),
                    buyer = UnionAddressConverter.convert(blockchain, source.right.maker),
                    priceUsd = priceUsd,
                    price = source.price,
                    type = OrderMatchSellDto.Type.SELL,
                    // TODO FLOW here should be price from FLOW, we don't want to calculate it here
                    amountUsd = amountUsd(priceUsd, source.left.asset),
                    // TODO FLOW there is no order info in flow for sides
                    sellerOrderHash = null,
                    buyerOrderHash = null,
                    transactionHash = source.transactionHash,
                    // TODO UNION remove in 1.19
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = source.transactionHash,
                        blockHash = source.blockHash,
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    ),
                    source = OrderActivitySourceDto.RARIBLE
                )
            }
            is FlowNftOrderActivityListDto -> {
                val payment = FlowConverter.convert(source.take, blockchain)
                val priceUsd = currencyService
                    .toUsd(blockchain, payment.type, source.price) ?: BigDecimal.ZERO

                OrderListActivityDto(
                    id = activityId,
                    date = source.date,
                    price = source.price,
                    // TODO FLOW here should be price from FLOW, we don't want to calculate it here
                    priceUsd = priceUsd,
                    hash = source.hash,
                    maker = UnionAddressConverter.convert(blockchain, source.maker),
                    make = FlowConverter.convert(source.make, blockchain),
                    take = FlowConverter.convert(source.take, blockchain)
                )
            }
            is FlowNftOrderActivityCancelListDto -> {
                OrderCancelListActivityDto(
                    id = activityId,
                    date = source.date,
                    hash = source.hash,
                    maker = UnionAddressConverter.convert(blockchain, source.maker),
                    make = FlowConverter.convertToType(source.make, blockchain),
                    take = FlowConverter.convertToType(source.take, blockchain),
                    transactionHash = source.transactionHash ?: "",
                    // TODO UNION remove in 1.19
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = source.transactionHash ?: "",
                        blockHash = source.blockHash ?: "",
                        blockNumber = source.blockNumber ?: 0,
                        logIndex = source.logIndex ?: 0
                    )
                )
            }
            is FlowMintDto -> {
                MintActivityDto(
                    id = activityId,
                    date = source.date,
                    owner = UnionAddressConverter.convert(blockchain, source.owner),
                    contract = ContractAddressConverter.convert(blockchain, source.contract),
                    tokenId = source.tokenId,
                    value = source.value,
                    transactionHash = source.transactionHash,
                    // TODO UNION remove in 1.19
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = source.transactionHash,
                        blockHash = source.blockHash,
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    )
                )
            }
            is FlowBurnDto -> {
                BurnActivityDto(
                    id = activityId,
                    date = source.date,
                    owner = UnionAddressConverter.convert(blockchain, source.owner),
                    contract = ContractAddressConverter.convert(blockchain, source.contract),
                    tokenId = source.tokenId,
                    value = source.value,
                    transactionHash = source.transactionHash,
                    // TODO UNION remove in 1.19
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = source.transactionHash,
                        blockHash = source.blockHash,
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    )
                )
            }
            is FlowTransferDto -> {
                TransferActivityDto(
                    id = activityId,
                    date = source.date,
                    from = UnionAddressConverter.convert(blockchain, source.from),
                    owner = UnionAddressConverter.convert(blockchain, source.owner),
                    contract = ContractAddressConverter.convert(blockchain, source.contract),
                    tokenId = source.tokenId,
                    value = source.value,
                    transactionHash = source.transactionHash,
                    // TODO UNION remove in 1.19
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = source.transactionHash,
                        blockHash = source.blockHash,
                        blockNumber = source.blockNumber,
                        logIndex = source.logIndex
                    )
                )
            }
        }
    }

    private fun amountUsd(price: BigDecimal, asset: FlowAssetDto) = price.multiply(asset.value)

    suspend fun convert(source: FlowActivitiesDto, blockchain: BlockchainDto): Slice<ActivityDto> {
        return Slice(
            continuation = source.continuation,
            entities = source.items.map { convert(it, blockchain) }
        )
    }
}