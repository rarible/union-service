package com.rarible.protocol.union.enrichment.meta.embedded

import com.rarible.protocol.union.core.util.safeSplit
import com.rarible.protocol.union.enrichment.configuration.EmbeddedContentProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Deprecated("Should be removed after complete migration of embedded content to Union")
@Component
class LegacyEmbeddedContentUrlDetector(
    properties: EmbeddedContentProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        logger.info("LegacyUrls: ${properties.legacyUrls}")
        logger.info("PublicUrl: ${properties.publicUrl}")
    }

    val legacyUrlPrefixes = safeSplit(properties.legacyUrls)

    fun isLegacyEmbeddedContentUrl(url: String): Boolean {
        legacyUrlPrefixes.forEach {
            if (url.startsWith(it)) {
                return true
            }
        }
        return false
    }

}