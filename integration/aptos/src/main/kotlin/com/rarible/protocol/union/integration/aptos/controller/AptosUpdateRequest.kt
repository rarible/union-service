package com.rarible.protocol.union.integration.aptos.controller

import com.rarible.protocol.dto.aptos.CollectionDto
import com.rarible.protocol.dto.aptos.OwnershipDto
import com.rarible.protocol.dto.aptos.TokenDto

data class AptosUpdateRequest(
    val tokens: List<TokenDto> = emptyList(),
    val ownerships: List<OwnershipDto> = emptyList(),
    val collections: List<CollectionDto> = emptyList()
)
