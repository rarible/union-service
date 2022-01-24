package com.rarible.protocol.union.listener.handler.wrapped

import com.rarible.protocol.union.core.handler.InternalEventHandler
import com.rarible.protocol.union.core.model.UnionWrappedEvent
import com.rarible.protocol.union.core.model.UnionWrappedItemEvent
import com.rarible.protocol.union.core.model.UnionWrappedOrderEvent
import com.rarible.protocol.union.core.model.UnionWrappedOwnershipEvent
import org.springframework.stereotype.Component

/**
 * Internal wrapped handler for item-related events (item/ownership/order)
 */
@Component
class UnionWrappedEventHandler(
    private val wrappedItemEventHandler: UnionWrappedItemEventHandler,
    private val wrappedOwnershipEventHandler: UnionWrappedOwnershipEventHandler,
    private val wrappedOrderEventHandler: UnionWrappedOrderEventHandler
) : InternalEventHandler<UnionWrappedEvent> {

    override suspend fun handle(event: UnionWrappedEvent) {
        when (event) {
            is UnionWrappedItemEvent -> wrappedItemEventHandler.onEvent(event.event)
            is UnionWrappedOwnershipEvent -> wrappedOwnershipEventHandler.onEvent(event.event)
            is UnionWrappedOrderEvent -> wrappedOrderEventHandler.onEvent(event.event)
        }
    }
}