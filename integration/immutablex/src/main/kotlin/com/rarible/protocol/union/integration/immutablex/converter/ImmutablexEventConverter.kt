package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.L2DepositActivityDto
import com.rarible.protocol.union.dto.L2WithdrawalActivityDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexDeposit
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexEvent
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexMint
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexTrade
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexTransfer
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexWithdrawal
import com.rarible.protocol.union.integration.immutablex.service.ImmutablexOrderService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import scalether.domain.Address

class ImmutablexEventConverter(
    private val orderService: ImmutablexOrderService,
) {

    suspend fun convert(event: ImmutablexEvent, blockchain: BlockchainDto = BlockchainDto.IMMUTABLEX): ActivityDto {
        val id = event.activityId
        return when (event) {
            is ImmutablexMint -> MintActivityDto(
                id = id,
                date = event.timestamp,
                owner = UnionAddressConverter.convert(blockchain, event.user),
                value = event.token.data.quantity,
                transactionHash = "${event.transactionId}",
                itemId = ItemIdDto(blockchain, event.itemId())
            )

            is ImmutablexTransfer -> {
                val from = UnionAddressConverter.convert(blockchain, event.user)
                val to = UnionAddressConverter.convert(blockchain, event.receiver)
                if (to.value == Address.ZERO().toString()) {
                    BurnActivityDto(
                        id = id,
                        date = event.timestamp,
                        owner = from,
                        value = event.token.data.quantity,
                        transactionHash = "${event.transactionId}",
                        itemId = ItemIdDto(blockchain, event.itemId())
                    )
                } else {
                    TransferActivityDto(
                        id = id,
                        date = event.timestamp,
                        from = from,
                        owner = to,
                        value = event.token.data.quantity,
                        transactionHash = "${event.transactionId}",
                        itemId = ItemIdDto(blockchain, event.itemId())
                    )
                }
            }
            is ImmutablexDeposit -> L2DepositActivityDto(
                id = id,
                date = event.timestamp,
                user = UnionAddressConverter.convert(blockchain, event.user),
                status = event.status,
                itemId = ItemIdDto(blockchain, event.itemId()),
                value = event.token.data.quantity
            )
            is ImmutablexTrade -> {
                val (makeOrder, takeOrder) = runBlocking(Dispatchers.IO) {
                    orderService.getOrderById("${event.make.orderId}") to orderService.getOrderById("${event.take.orderId}")
                }

                OrderMatchSellDto(
                    id = id, source = OrderActivitySourceDto.IMMUTABLEX,
                    transactionHash = "${event.transactionId}",
                    date = event.timestamp,
                    nft = makeOrder.make,
                    payment = takeOrder.make,
                    buyer = makeOrder.maker,
                    seller = takeOrder.maker,
                    price = makeOrder.makePrice!!,
                    type = OrderMatchSellDto.Type.SELL,
                )
            }
            is ImmutablexWithdrawal -> L2WithdrawalActivityDto(
                id = id,
                date = event.timestamp,
                user = UnionAddressConverter.convert(blockchain, event.sender),
                status = event.status,
                itemId = ItemIdDto(blockchain, event.token.data.itemId()),
                value = event.token.data.quantity
            )
        }

    }
}
