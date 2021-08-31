package com.rarible.protocol.union.dto.serializer.flow

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.FlowOwnershipIdDto

object FlowOwnershipIdSerializer : StdSerializer<FlowOwnershipIdDto>(FlowOwnershipIdDto::class.java) {

    override fun serialize(id: FlowOwnershipIdDto?, gen: JsonGenerator, provider: SerializerProvider) {
        if (id == null) {
            gen.writeNull()
            return
        }
        gen.writeStartObject()
        gen.writeStringField(FlowOwnershipIdDto::value.name, "${BlockchainDto.FLOW}:${id.value}");
        provider.defaultSerializeField(FlowOwnershipIdDto::token.name, id.token, gen)
        provider.defaultSerializeField(FlowOwnershipIdDto::tokenId.name, id.tokenId, gen)
        provider.defaultSerializeField(FlowOwnershipIdDto::owner.name, id.owner, gen)
        gen.writeEndObject()
    }
}