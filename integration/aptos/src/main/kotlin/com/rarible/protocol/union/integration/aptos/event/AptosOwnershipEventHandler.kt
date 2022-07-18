package com.rarible.protocol.union.integration.aptos.event

import com.rarible.protocol.dto.aptos.AptosOwnershipDeleteEventDto
import com.rarible.protocol.dto.aptos.AptosOwnershipEventDto
import com.rarible.protocol.dto.aptos.AptosOwnershipUpdateEventDto
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.group
import com.rarible.protocol.union.integration.aptos.converter.AptosOwnershipConverter
import java.math.BigInteger
import org.slf4j.LoggerFactory

class AptosOwnershipEventHandler(override val handler: IncomingEventHandler<UnionOwnershipEvent>) :
    AbstractBlockchainEventHandler<AptosOwnershipEventDto, UnionOwnershipEvent>(BlockchainDto.APTOS) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: AptosOwnershipEventDto) {
        logger.info("Received {} Ownership event: {}", blockchain, event)

        when(event) {
            is AptosOwnershipUpdateEventDto -> {
                val ownership = AptosOwnershipConverter.convert(event.ownership, blockchain)
                handler.onEvent(UnionOwnershipUpdateEvent(ownership))
            }
            is AptosOwnershipDeleteEventDto -> {
                val ownershipId = OwnershipIdDto(
                    blockchain = blockchain,
                    contract = event.ownership.contract!!,
                    tokenId = BigInteger(event.ownership.tokenId.toByteArray()),
                    owner = UnionAddress(blockchain.group(), event.ownership.owner)
                )
                handler.onEvent(UnionOwnershipDeleteEvent(ownershipId))
            }
        }
    }
}
