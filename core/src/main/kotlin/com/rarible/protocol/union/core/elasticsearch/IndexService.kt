package com.rarible.protocol.union.core.elasticsearch

import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.elasticsearch.EsHelper.getRealName
import com.rarible.protocol.union.core.elasticsearch.EsHelper.moveAlias
import com.rarible.protocol.union.core.model.EsMetadata
import com.rarible.protocol.union.core.model.elasticsearch.CurrentEntityDefinition
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.stereotype.Service

@Service
class IndexService(
    private val reactiveElasticSearchOperations: ReactiveElasticsearchOperations,
    private val esMetadataRepository: EsMetadataRepository,
    esNameResolver: EsNameResolver
) {
    private val metadataIndexName: String = esNameResolver.metadataIndexName

    suspend fun updateMetadata(entityDefinition: EntityDefinitionExtended) {
        innerUpdateMetadata(entityDefinition)
    }

    private suspend fun innerUpdateMetadata(entityDefinition: EntityDefinitionExtended) {
        val mappingId = EsEntityMetadataType.MAPPING.getId(entityDefinition)
        val mappingMetadata = esMetadataRepository.findById(mappingId)
        esMetadataRepository.save(
            mappingMetadata?.copy(content = entityDefinition.mapping)
                ?: EsMetadata(mappingId, entityDefinition.mapping)
        )

        val settingsId = EsEntityMetadataType.SETTINGS.getId(entityDefinition)
        val settingsMetadata = esMetadataRepository.findById(settingsId)
        esMetadataRepository.save(
            settingsMetadata?.copy(content = entityDefinition.settings)
                ?: EsMetadata(settingsId, entityDefinition.settings)
        )

        val versionId = EsEntityMetadataType.VERSION_DATA.getId(entityDefinition)
        val versionMetadata = esMetadataRepository.findById(versionId)
        esMetadataRepository.save(
            versionMetadata?.copy(content = entityDefinition.versionData.toString())
                ?: EsMetadata(versionId, entityDefinition.versionData.toString())
        )

        reactiveElasticSearchOperations
            .execute { it.indices().refreshIndex(RefreshRequest(metadataIndexName)) }
            .awaitFirstOrNull()
    }

    suspend fun getEntityMetadata(
        definition: EntityDefinitionExtended,
        realIndexName: String
    ): CurrentEntityDefinition {
        val id = EsEntityMetadataType.MAPPING.getId(definition)
        var response: EsMetadata = esMetadataRepository.findById(id)
            ?: throw RuntimeException("Index ${definition.entity} exists with name $realIndexName but metadata does not. Update metadata")

        val mapping = response.content

        response = esMetadataRepository.findById(EsEntityMetadataType.SETTINGS.getId(definition))!!
        val settings: String = response.content

        response = esMetadataRepository.findById(EsEntityMetadataType.VERSION_DATA.getId(definition))!!
        val version: Int = response.content.toInt()

        return CurrentEntityDefinition(
            mapping = mapping,
            settings = settings,
            versionData = version
        )
    }

    suspend fun finishIndexing(newIndexName: String, definition: EntityDefinitionExtended) {
        val alias = definition.aliasName
        val realIndexName = getRealName(reactiveElasticSearchOperations, alias, definition)
            ?: throw IllegalStateException("Index not found")
        if (realIndexName != newIndexName) {
            moveAlias(reactiveElasticSearchOperations, alias, realIndexName, newIndexName, definition)
        }
        updateMetadata(definition)
    }

    companion object {
        private val logger by Logger()
    }
}
