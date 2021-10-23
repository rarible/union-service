package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.union.core.ethereum.converter.EthConverter
import com.rarible.protocol.union.core.ethereum.converter.EthOwnershipConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.listener.handler.BlockchainEventHandler
import com.rarible.protocol.union.listener.service.EnrichmentOwnershipEventService
import org.slf4j.LoggerFactory

class EthereumOwnershipEventHandler(
    override val blockchain: BlockchainDto,
    private val ownershipEventService: EnrichmentOwnershipEventService
) : BlockchainEventHandler<NftOwnershipEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: NftOwnershipEventDto) {
        logger.debug("Received Ethereum ({}) Ownership event: type={}", blockchain, event::class.java.simpleName)

        when (event) {
            is NftOwnershipUpdateEventDto -> {
                val item = EthOwnershipConverter.convert(event.ownership, blockchain)
                ownershipEventService.onOwnershipUpdated(item)
            }
            is NftOwnershipDeleteEventDto -> {
                val ownershipId = OwnershipIdDto(
                    blockchain = blockchain,
                    token = UnionAddress(blockchain, EthConverter.convert(event.ownership.token)),
                    tokenId = event.ownership.tokenId,
                    owner = UnionAddress(blockchain, EthConverter.convert(event.ownership.owner))
                )
                ownershipEventService.onOwnershipDeleted(ownershipId)
            }
        }
    }
}
