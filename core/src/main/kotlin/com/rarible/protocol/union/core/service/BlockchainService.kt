package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.dto.BlockchainDto

interface BlockchainService {

    fun getBlockchain(): BlockchainDto

}