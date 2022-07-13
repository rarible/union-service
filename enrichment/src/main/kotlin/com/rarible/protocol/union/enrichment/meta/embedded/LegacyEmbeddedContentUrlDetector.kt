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
    private val fixedLegacy = safeSplit(properties.legacyUrls).lastOrNull()

    val legacyUrlPrefixes = safeSplit(properties.legacyUrls)

    init {

        logger.info(buildString {
            append("LegacyUrls: ${properties.legacyUrls}, ")
            append("PublicUrl: ${properties.publicUrl}, ")
            append("FixedLegacy: $fixedLegacy")
        })
    }

    fun fixLegacy(url: String): String {
        if (fixedLegacy != null) {
            for (legacy in legacyUrlPrefixes) {
                if (url.startsWith(legacy)) {
                    return url.replaceFirst(legacy, fixedLegacy)
                }
            }
        }
        return url
    }

    fun isLegacyEmbeddedContentUrl(url: String): Boolean {
        legacyUrlPrefixes.forEach {
            if (url.startsWith(it)) {
                return true
            }
        }
        return false
    }
}