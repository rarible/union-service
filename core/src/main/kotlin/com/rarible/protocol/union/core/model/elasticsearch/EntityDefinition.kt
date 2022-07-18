package com.rarible.protocol.union.core.model.elasticsearch

import java.math.BigInteger
import java.security.MessageDigest

data class EntityDefinition(
    val entity: EsEntity,
    val mapping: String,
    val versionData: Int,
    val settings: String
) {
    val reindexTask = "${entity.name}_REINDEX"
    val settingsHash = md5(settings)
}

fun md5(input: String): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
}