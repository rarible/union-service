package com.rarible.protocol.union.listener.handler

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionAuctionEvent
import com.rarible.protocol.union.core.model.UnionAuctionUpdateEvent
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = "app", subtype = "event")
class UnionAuctionEventHandler(
//    private val auctionEventService: EnrichmentAuctionEventService
) : IncomingEventHandler<UnionAuctionEvent> {

    override suspend fun onEvent(event: UnionAuctionEvent) {
        when (event) {
            is UnionAuctionUpdateEvent -> {
                // TODO
            }
        }
    }
}
