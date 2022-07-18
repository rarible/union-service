package com.rarible.protocol.union.core.model.elasticsearch

import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates

data class EntityDefinitionExtended(
    val entity: EsEntity,
    val mapping: String,
    val versionData: Int,
    val indexRootName: String,
    val aliasName: String,
    val writeAliasName: String,
    val settings: String,
    val reindexTask: String,
    val settingsHash: String,
) {
    val searchIndexCoordinates: IndexCoordinates = IndexCoordinates.of(aliasName)

    val writeIndexCoordinates: IndexCoordinates = IndexCoordinates.of(aliasName, writeAliasName)

    fun indexName(minorVersion: Int) = "$indexRootName$minorVersion"

    fun getVersion(realIndexName: String): Int {
        val version = realIndexName.replace(indexRootName, "")
        return if (version.isBlank()) 1 else version.toInt()
    }
}
