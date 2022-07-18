package com.rarible.protocol.union.integration.aptos.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.protocol.dto.aptos.RoyaltiesDto
import com.rarible.protocol.dto.aptos.RoyaltyDto

class AptosRoyaltiesDeserializer: JsonDeserializer<RoyaltiesDto>() {

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): RoyaltiesDto {
        val a: ArrayNode = p.codec.readTree(p)
        val list = a.map {
            mapper.readValue(it.asText(), RoyaltyDto::class.java)
        }
        return RoyaltiesDto(royalties = list)
    }
}
