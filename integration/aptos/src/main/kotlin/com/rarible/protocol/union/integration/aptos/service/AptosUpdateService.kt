package com.rarible.protocol.union.integration.aptos.service

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.aptos.AptosCollectionUpdateEventDto
import com.rarible.protocol.dto.aptos.AptosOwnershipUpdateEventDto
import com.rarible.protocol.dto.aptos.AptosTokenDeleteEventDto
import com.rarible.protocol.dto.aptos.AptosTokenUpdateEventDto
import com.rarible.protocol.dto.aptos.CollectionDto
import com.rarible.protocol.dto.aptos.OwnershipDto
import com.rarible.protocol.dto.aptos.TokenDto
import com.rarible.protocol.union.integration.aptos.event.AptosCollectionEventHandler
import com.rarible.protocol.union.integration.aptos.event.AptosItemEventHandler
import com.rarible.protocol.union.integration.aptos.event.AptosOwnershipEventHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AptosUpdateService(
    private val itemEventHandler: AptosItemEventHandler,
    private val ownershipEventHandler: AptosOwnershipEventHandler,
    private val collectionEventHandler: AptosCollectionEventHandler
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    suspend fun updateTokens(tokens: List<TokenDto>) {
        logger.info("Update ${tokens.size} APTOS tokens")
        tokens.forEach { token ->
            try {
                val eventId = "${token.id}_${nowMillis()}"
                val event = if (token.deleted == true) {
                    AptosTokenDeleteEventDto(
                        eventId = eventId,
                        itemId = token.id,
                        item = token
                    )
                } else AptosTokenUpdateEventDto(eventId, token.id, token)
                itemEventHandler.handle(event)
            } catch (e: Exception) {
                logger.error("Unable to handle APTOS token $token", e.message, e)
                throw e
            }
        }
    }

    suspend fun updateOwnerships(ownerships: List<OwnershipDto>) {
        logger.info("Update ${ownerships.size} APTOS ownerships")
        ownerships.forEach {ownership ->
            try {
                val eventId = "${ownership.id}_${nowMillis()}"
                ownershipEventHandler.handle(AptosOwnershipUpdateEventDto(eventId, ownership.id, ownership))
            } catch (e: Exception) {
                logger.error("Unable to handle APTOS ownership $ownership", e.message, e)
                throw e
            }
        }
    }

    suspend fun updateCollections(collections: List<CollectionDto>) {
        logger.info("Update ${collections.size} APTOS collections")
        collections.forEach {collection ->
            try {
                val eventId = "${collection.id}_${nowMillis()}"
                collectionEventHandler.handle(AptosCollectionUpdateEventDto(eventId, collection.id, collection))
            } catch (e: Exception) {
                logger.error("Unable to handle APTOS collection $collection", e.message, e)
                throw e
            }
        }
    }
}
