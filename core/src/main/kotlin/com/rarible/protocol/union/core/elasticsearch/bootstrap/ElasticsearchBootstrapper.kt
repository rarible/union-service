package com.rarible.protocol.union.core.elasticsearch.bootstrap

import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.elasticsearch.EsHelper.createAlias
import com.rarible.protocol.union.core.elasticsearch.EsHelper.createIndex
import com.rarible.protocol.union.core.elasticsearch.EsHelper.existsIndex
import com.rarible.protocol.union.core.elasticsearch.EsHelper.getIndexesByAlias
import com.rarible.protocol.union.core.elasticsearch.EsHelper.getRealName
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver.Companion.METADATA_INDEX
import com.rarible.protocol.union.core.elasticsearch.EsRepository
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.core.elasticsearch.ReindexSchedulingService
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinition
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.client.indices.PutMappingRequest
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.XContentType
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations

class ElasticsearchBootstrapper(
    private val esNameResolver: EsNameResolver,
    private val esOperations: ReactiveElasticsearchOperations,
    entityDefinitions: List<EntityDefinition>,
    private val reindexSchedulingService: ReindexSchedulingService,
    private val indexService: IndexService,
    private val repositories: List<EsRepository>,
    private val restHighLevelClient: RestHighLevelClient,
) {
    private val metadataMapping = metadataMappingIndex()
    private val metadataSettings = metadataSettingsIndex()

    private val extendedEntityDefinitions: List<EntityDefinitionExtended> =
        entityDefinitions.map { esNameResolver.createEntityDefinitionExtended(it) }

    fun bootstrap() = runBlocking {

        logger.info("Initializing elasticsearch")
        setupCluster()
        createIndex(
            reactiveElasticSearchOperations = esOperations,
            name = esNameResolver.metadataIndexName,
            mapping = metadataMapping,
            settings = metadataSettings,
        )
        for (definition in extendedEntityDefinitions) {
            logger.info("Updating index for entity ${definition.entity}")
            updateIndexMetadata(definition)
        }
        repositories.forEach { it.init() }
        logger.info("Finished elasticsearch initialization")
    }

    private fun setupCluster() {
        val request = ClusterUpdateSettingsRequest()

        val persistentSettings: Settings = Settings.builder()
            .put("action.auto_create_index", "false")
            .build()

        request.persistentSettings(persistentSettings)
        logger.info("Setting up cluster with persistent settings: $persistentSettings")
        restHighLevelClient.cluster().putSettings(request, RequestOptions.DEFAULT)
        logger.info("Settings applied")
    }

    private suspend fun checkWriteIndexIsCreated(writeAlias: String): Boolean {
        val exists = esOperations.execute { it.indices().existsIndex(GetIndexRequest(writeAlias)) }.awaitFirst()

        if (!exists) return false

        val currentWriteIndices =
            esOperations.execute { it.indices().getIndex(GetIndexRequest(writeAlias)) }
                .awaitFirst()
                .aliases.keys
        if (currentWriteIndices.size > 1) {
            logger.info("Reindex already in progress for index $writeAlias")
            return true
        }
        return false
    }

    private suspend fun updateIndexMetadata(definition: EntityDefinitionExtended) {
        logger.info("Attempt to update index metadata, definition = $definition")
        val realIndexName = getRealName(esOperations, definition.aliasName, definition)
        logger.info("Real index name = $realIndexName")

        if (realIndexName == null) {
            createFirstIndex(definition)
            return
        }
        val currentEntityMetadata = indexService.getEntityMetadata(definition, realIndexName)
        logger.info("Current entity metadata = $currentEntityMetadata")
        when {
            currentEntityMetadata.settings != definition.settings ||
                currentEntityMetadata.versionData != definition.versionData -> {
                if (reindexSchedulingService.checkReindexInProgress(definition)) {
                    logger.info("Reindexing tasks for entity ${definition.entity} was started")
                    return
                }
                reindexSchedulingService.stopTasksIfExists(definition)
                val newIndexName = definition.indexName(minorVersion = definition.versionData)
                if (existsIndex(esOperations, newIndexName) &&
                    getIndexesByAlias(esOperations, definition.writeAliasName).contains(newIndexName)
                ) {
                    logger.info("Updating index for entity ${definition.entity} is in progress. Stop task and recreate")
                    scheduleReindex(definition, realIndexName)
                } else {
                    val indexVersion = definition.getVersion(realIndexName)
                    val newIndexName = definition.indexName(minorVersion = indexVersion + 1)
                    recreateIndex(realIndexName, newIndexName, definition)
                }
            }
            currentEntityMetadata.mapping != definition.mapping -> {
                updateMappings(
                    realIndexName = getRealName(esOperations, definition.writeAliasName, definition)
                        ?: throw IllegalStateException("Not exists index for ${definition.writeAliasName}"),
                    definition = definition
                )
                if (reindexSchedulingService.checkReindexInProgress(definition)) {
                    logger.info("Reindexing tasks for entity ${definition.entity} was started")
                    return
                }
                reindexSchedulingService.stopTasksIfExists(definition)
                scheduleReindex(definition, realIndexName)
            }
            else -> logger.info("Index ${definition.entity} mapping and settings has not changed. Skipping index update")
        }
    }

    private suspend fun createFirstIndex(definition: EntityDefinitionExtended) {
        logger.info("Creating index for first time")
        val newIndexName = definition.indexName(minorVersion = definition.versionData)
        createIndex(
            reactiveElasticSearchOperations = esOperations,
            name = newIndexName,
            mapping = definition.mapping,
            settings = definition.settings
        )
        createAlias(
            reactiveElasticSearchOperations = esOperations,
            indexName = newIndexName,
            alias = definition.aliasName
        )
        createAlias(
            reactiveElasticSearchOperations = esOperations,
            indexName = newIndexName,
            alias = definition.writeAliasName
        )
        scheduleReindex(
            definition = definition,
            newIndexName = newIndexName
        )
    }

    private suspend fun updateMappings(
        realIndexName: String,
        definition: EntityDefinitionExtended
    ) {
        logger.info("Index ${definition.entity} mappings changed. Updating")
        esOperations.execute {
            it.indices().putMapping(
                PutMappingRequest(realIndexName)
                    .source(definition.mapping, XContentType.JSON)
            )
        }
            .asFlow()
            .catch {
                logger.info("Failed to update index ${definition.entity} mapping. Recreating index")
                val indexVersion = definition.getVersion(realIndexName)
                val newIndexName = definition.indexName(minorVersion = indexVersion + 1)
                recreateIndex(realIndexName, newIndexName, definition)
            }
            .toList()
    }

    private suspend fun recreateIndex(
        realIndexName: String,
        newIndexName: String,
        definition: EntityDefinitionExtended,
    ) {
        logger.info("Recreating index $realIndexName with definition = $definition")

        createIndex(
            reactiveElasticSearchOperations = esOperations,
            name = newIndexName,
            mapping = definition.mapping,
            settings = definition.settings
        )
        createAlias(
            reactiveElasticSearchOperations = esOperations,
            indexName = newIndexName,
            alias = definition.writeAliasName
        )
        scheduleReindex(definition, newIndexName)
    }

    private fun scheduleReindex(
        definition: EntityDefinitionExtended,
        newIndexName: String
    ) = runBlocking {
        logger.info("Scheduling reindex for ${definition.entity} to $newIndexName")
        reindexSchedulingService.scheduleReindex(newIndexName, definition)
    }

    companion object {
        private val logger by Logger()
    }
}

fun metadataMappingIndex(): String {
    return ElasticsearchBootstrapper::class.java.getResource("/mappings/${METADATA_INDEX}.json")!!.readText()
}

fun metadataSettingsIndex(): String {
    val url = ElasticsearchBootstrapper::class.java.getResource("/mappings/${METADATA_INDEX}_settings.json")
    return url?.readText() ?: "{}"
}
