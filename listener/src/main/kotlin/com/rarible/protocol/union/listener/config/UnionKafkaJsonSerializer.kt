package com.rarible.protocol.union.listener.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.protocol.union.dto.serializer.UnionModelJacksonModule
import com.rarible.protocol.union.dto.serializer.UnionPrimitivesJacksonModule

class UnionKafkaJsonSerializer : JsonSerializer() {

    override fun createMapper(): ObjectMapper {
        return super.createMapper()
            .registerModule(UnionPrimitivesJacksonModule)
            .registerModule(UnionModelJacksonModule)
    }
}