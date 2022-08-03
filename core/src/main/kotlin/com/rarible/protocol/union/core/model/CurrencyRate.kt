package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.BlockchainDto
import java.math.BigDecimal

data class CurrencyRate(
    val blockchain: BlockchainDto,
    val currencyId: String, // blockchain:address
    val rate: BigDecimal
)