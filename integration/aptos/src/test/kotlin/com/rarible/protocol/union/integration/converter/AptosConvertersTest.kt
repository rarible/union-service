package com.rarible.protocol.union.integration.converter

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.protocol.dto.aptos.CollectionDto
import com.rarible.protocol.dto.aptos.OwnershipDto
import com.rarible.protocol.dto.aptos.RoyaltiesDto
import com.rarible.protocol.dto.aptos.TokenDto
import com.rarible.protocol.dto.aptos.TokenMetaDataDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.aptos.converter.AptosCollectionConverter
import com.rarible.protocol.union.integration.aptos.converter.AptosItemConverter
import com.rarible.protocol.union.integration.aptos.converter.AptosItemMetaConverter
import com.rarible.protocol.union.integration.aptos.converter.AptosOwnershipConverter
import com.rarible.protocol.union.integration.aptos.deserializer.AptosRoyaltiesDeserializer
import java.math.BigInteger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

class AptosConvertersTest {

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    private val blockchain = BlockchainDto.APTOS

    @Test
    internal fun `should convert token`() {
        val module = SimpleModule()
        module.addDeserializer(RoyaltiesDto::class.java, AptosRoyaltiesDeserializer())
        mapper.registerModule(module)
        val source = ClassPathResource("json/aptos-token.json").inputStream.use {
            mapper.readValue(it,TokenDto::class.java)
        }

        val item = AptosItemConverter.convert(source, blockchain)

        assertThat(item.id.fullId()).isEqualTo("${blockchain}:${source.id}")
        assertThat(item.creators).isNotEmpty
        val (creator, collectionName, _) = source.id.split("::")
        assertThat(item.collection?.fullId()).isEqualTo("${blockchain}:${creator}::$collectionName")
        assertThat(item.lazySupply).isEqualTo(BigInteger.ZERO)
        assertThat(item.mintedAt).isEqualTo(source.mintedAt)
        assertThat(item.deleted).isFalse
        assertThat(item.supply).isEqualTo(source.supply.toBigInteger())
    }

    @Test
    internal fun `should convert ownership`() {
        val source = ClassPathResource("json/aptos-ownership.json").inputStream.use {
            mapper.readValue(it, OwnershipDto::class.java)
        }

        val ownership = AptosOwnershipConverter.convert(source, blockchain)

        assertThat(ownership.id.fullId()).isEqualTo("${blockchain}:${source.tokenId}:${source.owner}")
        assertThat(ownership.collection?.fullId()).isEqualTo("${blockchain}:${source.collection}")
        assertThat(ownership.value).isEqualTo(source.value.toBigInteger())
        assertThat(ownership.createdAt).isEqualTo(source.createdAt)
        assertThat(ownership.lazyValue).isEqualTo(BigInteger.ZERO)
        assertThat(ownership.creators).isNotEmpty
    }

    @Test
    internal fun `should convert collection`() {
        val source = ClassPathResource("json/aptos-collection.json").inputStream.use {
            mapper.readValue(it, CollectionDto::class.java)
        }

        val collection = AptosCollectionConverter.convert(source, blockchain)

        assertThat(collection.id.fullId()).isEqualTo("${blockchain}:${source.id}")
        assertThat(collection.name).isEqualTo(source.name)
        assertThat(collection.type).isEqualTo(com.rarible.protocol.union.dto.CollectionDto.Type.APTOS)
        assertThat(collection.features).isEqualTo(listOf(com.rarible.protocol.union.dto.CollectionDto.Features.APPROVE_FOR_ALL))
        assertThat(collection.owner?.fullId()).isEqualTo("${blockchain}:${source.creator}")
        assertThat(collection.meta).isNotNull
        assertThat(collection.meta?.name).isEqualTo(source.name)
        assertThat(collection.meta?.description).isEqualTo(source.description)
    }

    @Test
    internal fun `should convert item meta`() {
        val source = ClassPathResource("json/aptos-meta.json").inputStream.use {
            mapper.readValue(it, TokenMetaDataDto::class.java)
        }

        val meta = AptosItemMetaConverter.convert(source, blockchain)

        assertThat(meta.name).isEqualTo(source.name)
        assertThat(meta.description).isEqualTo(source.description)
        assertThat(meta.content).isNotEmpty
        assertThat(meta.content.first().url).isEqualTo(source.content.first().url)
        assertThat(meta.attributes).isNotEmpty
        assertThat(meta.attributes.first().key).isEqualTo(source.attributes.first().key)
        assertThat(meta.attributes.first().value).isEqualTo(source.attributes.first().value)
        assertThat(meta.attributes.first().type).isEqualTo(source.attributes.first().type)
        assertThat(meta.attributes.first().format).isEqualTo(source.attributes.first().format)
    }
}
