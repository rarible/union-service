package com.rarible.protocol.union.integration.immutablex

import com.rarible.protocol.union.integration.immutablex.client.EventsApi
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexEvent
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexPage
import com.rarible.protocol.union.integration.immutablex.entity.ImmutablexState
import com.rarible.protocol.union.integration.immutablex.events.ImmutablexActivityEventHandler
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import kotlin.reflect.KProperty

class ImmutablexScanner(
    private val eventsApi: EventsApi,
    private val mongo: MongoTemplate,
    private val activityHandler: ImmutablexActivityEventHandler
) {

    private lateinit var info: ImmutablexState

    @PostConstruct
    fun postCreate() {
        info = mongo.findById(1L) ?: mongo.save(ImmutablexState(id = 1L))
    }

    @Scheduled(initialDelay = 1L, fixedDelay = 5L, timeUnit = TimeUnit.SECONDS)
    fun mints() {
        runBlocking { listen(eventsApi::mints, info::lastMintCursor) }
    }
    @Scheduled(initialDelay = 2L, fixedDelay = 5L, timeUnit = TimeUnit.SECONDS)
    fun transfers() {
        runBlocking { listen(eventsApi::transfers, info::lastTransferCursor) }
    }
    @Scheduled(initialDelay = 3L, fixedDelay = 5L, timeUnit = TimeUnit.SECONDS)
    fun trades() {
        runBlocking { listen(eventsApi::trades, info::lastTradesCursor) }
    }

    @Scheduled(initialDelay = 4L, fixedDelay = 5L, timeUnit = TimeUnit.SECONDS)
    fun deposits() {
        runBlocking { listen(eventsApi::deposits, info::lastDepositCursor) }
    }

    @Scheduled(initialDelay = 5L, fixedDelay = 5L, timeUnit = TimeUnit.SECONDS)
    fun withdrawals() {
        runBlocking { listen(eventsApi::withdrawals, info::lastWithdrawCursor) }
    }

    private suspend fun <T: ImmutablexEvent>listen(apiMethod: suspend (cursor: String?) -> ImmutablexPage<T>, cursorField: KProperty<String?>) {
        val page = apiMethod(cursorField.call())
        if (page.result.isNotEmpty()) {
            handle(page.result)
        }
        mongo.updateFirst(Query(
            where(ImmutablexState::id).isEqualTo(1L)
        ), Update().set(cursorField.name, page.cursor), ImmutablexState::class.java)

    }

    private suspend fun <T: ImmutablexEvent>handle(items: List<T>) {
        items.forEach {
            activityHandler.handle(it)
        }
    }
}
