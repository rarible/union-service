package com.rarible.protocol.union.core.elasticsearch

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinition
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.stereotype.Component

@Component
class EsNameResolver(
    private val applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {
    final val metadataIndexName = "${PREFIX}_${applicationEnvironmentInfo.name}_$METADATA_INDEX"

    val metadataIndexCoordinate: IndexCoordinates = IndexCoordinates.of(metadataIndexName)

    fun createEntityDefinitionExtended(entity: EntityDefinition) =
        EntityDefinitionExtended(
            entity = entity.entity,
            mapping = entity.mapping,
            versionData = entity.versionData,
            indexRootName = indexRootName(entity),
            aliasName = aliasName(entity),
            writeAliasName = writeAliasName(entity),
            settings = entity.settings,
            reindexTask = entity.reindexTask,
            settingsHash = entity.settingsHash
        )

    private fun indexRootName(entity: EntityDefinition) =
        "${PREFIX}_${applicationEnvironmentInfo.name}_${entity.entity.entityName}_"

    private fun aliasName(entity: EntityDefinition) =
        "${PREFIX}_${applicationEnvironmentInfo.name}_${entity.entity.entityName}_alias"

    private fun writeAliasName(entity: EntityDefinition) =
        "${PREFIX}_${applicationEnvironmentInfo.name}_${entity.entity.entityName}_write_alias"

    companion object {
        const val METADATA_INDEX = "metadata"
        private const val PREFIX = "protocol_union"
    }
}