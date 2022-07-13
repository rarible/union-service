package com.rarible.protocol.union.enrichment.meta.embedded

import com.rarible.protocol.union.enrichment.configuration.EmbeddedContentProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class LegacyEmbeddedContentUrlDetectorTest {
    private val properties = EmbeddedContentProperties(
        publicUrl = "new.com",
        legacyUrls = "wrong.com,right.com"
    )

    private val detector = LegacyEmbeddedContentUrlDetector(properties)

    @Test
    fun fixLegacyUrl() {
        val fixes = detector.fixLegacy("wrong.com")
        assertThat(fixes).isEqualTo("right.com")
    }
}