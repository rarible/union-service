package com.rarible.protocol.union.test.mock

import com.rarible.core.common.nowMillis
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.CurrencyRateDto
import com.rarible.protocol.union.core.service.CurrencyService
import io.mockk.coEvery
import io.mockk.mockk
import reactor.kotlin.core.publisher.toMono
import java.math.BigDecimal

object CurrencyMock {

    val currencyControllerApiMock: CurrencyControllerApi = mockk()
    val currencyServiceMock = CurrencyService(currencyControllerApiMock)

    init {
        coEvery {
            currencyControllerApiMock.getCurrencyRate(any(), any(), any())
        } answers {
            CurrencyRateDto(
                fromCurrencyId = it.invocation.args[1] as String,
                toCurrencyId = "usd",
                rate = BigDecimal.ONE,
                date = nowMillis()
            ).toMono()
        }
    }

}





