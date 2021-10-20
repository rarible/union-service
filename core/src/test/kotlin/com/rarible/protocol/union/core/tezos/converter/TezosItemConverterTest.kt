package com.rarible.protocol.union.core.tezos.converter

import com.rarible.core.test.data.randomString
import com.rarible.protocol.tezos.dto.NftItemAttributeDto
import com.rarible.protocol.tezos.dto.NftItemMetaDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ImageContentDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.VideoContentDto
import com.rarible.protocol.union.test.data.randomTezosNftItemDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TezosItemConverterTest {

    @Test
    fun `tezos item`() {
        val dto = randomTezosNftItemDto()

        val converted = TezosItemConverter.convert(dto, BlockchainDto.TEZOS)

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.id.token.value).isEqualTo(dto.contract)
        assertThat(converted.id.tokenId).isEqualTo(dto.tokenId)
        assertThat(converted.tokenId).isEqualTo(dto.tokenId)
        assertThat(converted.collection.value).isEqualTo(dto.contract)
        assertThat(converted.supply).isEqualTo(dto.supply)
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.date)
        assertThat(converted.mintedAt).isEqualTo(dto.date) // TODO not correct
        assertThat(converted.lazySupply).isEqualTo(dto.lazySupply)
        assertThat(converted.deleted).isEqualTo(dto.deleted)
        assertThat(converted.lazySupply).isEqualTo(dto.lazySupply)

        assertThat(converted.owners[0].value).isEqualTo(dto.owners[0])

        assertThat(converted.royalties[0].account.value).isEqualTo(dto.royalties[0].account)
        assertThat(converted.royalties[0].value).isEqualTo(dto.royalties[0].value.toBigDecimal())

        assertThat(converted.creators[0].account.value).isEqualTo(dto.creators[0].account)
        assertThat(converted.creators[0].value).isEqualTo(dto.creators[0].value.toBigDecimal())
    }

    @Test
    fun `tezos item meta`() {
        val item = randomTezosNftItemDto().copy(
            meta = NftItemMetaDto(
                name = "some_nft_meta",
                description = randomString(),
                attributes = listOf(
                    NftItemAttributeDto("key1", "value1"),
                    NftItemAttributeDto("key2", "value2")
                ),
                image = "url1",
                animation = "url2"
            )
        )
        val dto = item.meta!!

        val converted = TezosItemConverter.convert(item, BlockchainDto.TEZOS).meta!!

        assertThat(converted.name).isEqualTo(dto.name)
        assertThat(converted.description).isEqualTo(dto.description)
        assertThat(converted.content).hasSize(2)
        assertThat(converted.attributes.find { it.key == "key1" }?.value).isEqualTo("value1")
        assertThat(converted.attributes.find { it.key == "key2" }?.value).isEqualTo("value2")

        val originalImage = converted.content[0] as ImageContentDto
        val originalAnim = converted.content[1] as VideoContentDto

        assertThat(originalImage.url).isEqualTo("url1")
        assertThat(originalImage.representation).isEqualTo(MetaContentDto.Representation.ORIGINAL)
        assertThat(originalImage.mimeType).isEqualTo(null)
        assertThat(originalImage.width).isEqualTo(null)
        assertThat(originalImage.height).isEqualTo(null)

        assertThat(originalAnim.url).isEqualTo("url2")
        assertThat(originalAnim.representation).isEqualTo(MetaContentDto.Representation.ORIGINAL)
        assertThat(originalAnim.mimeType).isEqualTo(null)
        assertThat(originalAnim.width).isEqualTo(null)
        assertThat(originalAnim.height).isEqualTo(null)
    }
}