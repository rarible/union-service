package com.rarible.protocol.union.enrichment.converter

import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.protocol.union.core.model.UnionAudioProperties
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionModel3dProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.dto.ImageContentDto
import com.rarible.protocol.union.dto.VideoContentDto
import com.rarible.protocol.union.enrichment.test.data.randomUnionContent
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EnrichedMetaConverterTest {

    @Test
    fun `convert meta`() {
        val meta = randomUnionMeta()

        val converted = EnrichedMetaConverter.convert(meta)

        assertThat(converted.name).isEqualTo(meta.name)
        assertThat(converted.description).isEqualTo(meta.description)
        assertThat(converted.attributes).isEqualTo(meta.attributes)
        assertThat(converted.restrictions).isEqualTo(meta.restrictions.map { it.type })
    }

    @Test
    fun `convert image properties`() {
        val imageProperties = UnionImageProperties(
            mimeType = "image/png",
            size = randomLong(),
            width = randomInt(),
            height = randomInt()
        )
        val image = randomUnionContent(imageProperties)

        val meta = randomUnionMeta().copy(content = listOf(image))

        val converted = EnrichedMetaConverter.convert(meta)
        val convertedImage = converted.content[0] as ImageContentDto

        assertThat(convertedImage.url).isEqualTo(image.url)
        assertThat(convertedImage.representation).isEqualTo(image.representation)
        assertThat(convertedImage.mimeType).isEqualTo(imageProperties.mimeType)
        assertThat(convertedImage.size).isEqualTo(imageProperties.size)
        assertThat(convertedImage.width).isEqualTo(imageProperties.width)
        assertThat(convertedImage.height).isEqualTo(imageProperties.height)
    }

    @Test
    fun `convert video properties`() {
        val videoProperties = UnionVideoProperties(
            mimeType = "video/mp4",
            size = randomLong(),
            width = randomInt(),
            height = randomInt()
        )
        val video = randomUnionContent(videoProperties)

        val meta = randomUnionMeta().copy(content = listOf(video))

        val converted = EnrichedMetaConverter.convert(meta)
        val convertedVideo = converted.content[0] as VideoContentDto

        assertThat(convertedVideo.url).isEqualTo(video.url)
        assertThat(convertedVideo.representation).isEqualTo(video.representation)
        assertThat(convertedVideo.mimeType).isEqualTo(videoProperties.mimeType)
        assertThat(convertedVideo.size).isEqualTo(videoProperties.size)
        assertThat(convertedVideo.width).isEqualTo(videoProperties.width)
        assertThat(convertedVideo.height).isEqualTo(videoProperties.height)
    }

    @Test
    fun `convert audio properties`() {
        val audioProperties = UnionAudioProperties(
            mimeType = "audio/mp4",
            size = randomLong()
        )
        val audio = randomUnionContent(audioProperties)

        val meta = randomUnionMeta().copy(content = listOf(audio))

        val converted = EnrichedMetaConverter.convert(meta)
        // TODO replace with Audio when market support it
        val convertedAudio = converted.content[0] as VideoContentDto

        assertThat(convertedAudio.url).isEqualTo(audio.url)
        assertThat(convertedAudio.representation).isEqualTo(audio.representation)
        assertThat(convertedAudio.mimeType).isEqualTo(audioProperties.mimeType)
        assertThat(convertedAudio.size).isEqualTo(audioProperties.size)
    }

    @Test
    fun `convert model properties`() {
        val modelProperties = UnionModel3dProperties(
            mimeType = "model/gltf",
            size = randomLong()
        )
        val model = randomUnionContent(modelProperties)

        val meta = randomUnionMeta().copy(content = listOf(model))

        val converted = EnrichedMetaConverter.convert(meta)
        // TODO replace with Model3D when market support it
        val convertedAudio = converted.content[0] as VideoContentDto

        assertThat(convertedAudio.url).isEqualTo(model.url)
        assertThat(convertedAudio.representation).isEqualTo(model.representation)
        assertThat(convertedAudio.mimeType).isEqualTo(modelProperties.mimeType)
        assertThat(convertedAudio.size).isEqualTo(modelProperties.size)
    }

}