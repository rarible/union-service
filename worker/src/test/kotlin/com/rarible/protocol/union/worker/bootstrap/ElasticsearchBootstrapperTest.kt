package com.rarible.protocol.union.worker.bootstrap

import com.mongodb.assertions.Assertions.assertTrue
import com.rarible.protocol.union.core.elasticsearch.EsEntityMetadataType
import com.rarible.protocol.union.core.elasticsearch.EsHelper
import com.rarible.protocol.union.core.elasticsearch.EsHelper.getMapping
import com.rarible.protocol.union.core.elasticsearch.EsMetadataRepository
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.core.elasticsearch.ReindexSchedulingService
import com.rarible.protocol.union.core.elasticsearch.bootstrap.ElasticsearchBootstrapper
import com.rarible.protocol.union.core.elasticsearch.getId
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinition
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended
import com.rarible.protocol.union.worker.IntegrationTest
import io.mockk.clearMocks
import io.mockk.coVerify
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.indices.GetIndexRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations

@IntegrationTest
internal class ElasticsearchBootstrapperTest {
    @Autowired
    private lateinit var reactiveElasticSearchOperations: ReactiveElasticsearchOperations

    @Autowired
    private lateinit var esMetadataRepository: EsMetadataRepository

    @Autowired
    private lateinit var reindexSchedulingService: ReindexSchedulingService

    @Autowired
    private lateinit var esNameResolver: EsNameResolver

    @Autowired
    private lateinit var highLevelClient: RestHighLevelClient

    @Autowired
    private lateinit var indexService: IndexService

    @Autowired
    private lateinit var restHighLevelClient: ReactiveElasticsearchClient

    private val newVersionData = EsActivity.VERSION + 1
    private lateinit var entityDefinitions: List<EntityDefinition>
    private lateinit var entityDefinitionExtended: EntityDefinitionExtended
    private lateinit var bootstrapper: ElasticsearchBootstrapper
    private lateinit var newEntityDefinition: EntityDefinition
    private lateinit var newEntityDefinitionExtended: EntityDefinitionExtended
    private lateinit var newBootstrapper: ElasticsearchBootstrapper
    private lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    private val loadMapping = "{\n" +
        "  \"dynamic\": \"false\",\n" +
        "  \"properties\": {\n" +
        "    \"changedFieldForTest\": {\n" +
        "      \"type\": \"keyword\"\n" +
        "    }\n" +
        "  }\n" +
        "}"

    @BeforeEach
    fun init() = runBlocking<Unit> {
        clearMocks(reindexSchedulingService)
        entityDefinitions = listOf(EsActivity.ENTITY_DEFINITION)
        entityDefinitionExtended = esNameResolver.createEntityDefinitionExtended(EsActivity.ENTITY_DEFINITION)
        bootstrapper = ElasticsearchBootstrapper(
            esNameResolver = esNameResolver,
            esOperations = reactiveElasticSearchOperations,
            entityDefinitions = entityDefinitions,
            reindexSchedulingService,
            indexService,
            repositories = emptyList(),
            highLevelClient
        )
        newEntityDefinition = EntityDefinition(
            entityDefinitionExtended.entity,
            entityDefinitionExtended.mapping,
            newVersionData,
            entityDefinitionExtended.settings
        )
        newEntityDefinitionExtended = esNameResolver.createEntityDefinitionExtended(newEntityDefinition)
        newBootstrapper = ElasticsearchBootstrapper(
            esNameResolver = esNameResolver,
            esOperations = reactiveElasticSearchOperations,
            entityDefinitions = listOf(newEntityDefinition),
            reindexSchedulingService,
            indexService,
            repositories = emptyList(),
            highLevelClient
        )

        elasticsearchTestBootstrapper = ElasticsearchTestBootstrapper(
            esNameResolver = esNameResolver,
            esOperations = reactiveElasticSearchOperations,
            restHighLevelClient,
            entityDefinitions = entityDefinitions,
            repositories = emptyList()
        )

        elasticsearchTestBootstrapper.deleteAllIndexes()
    }

    @Test
    fun `should create first index`() = runBlocking<Unit> {
        bootstrapper.bootstrap()
        val indexName = entityDefinitionExtended.indexName(entityDefinitionExtended.versionData)
        val aliasName = entityDefinitionExtended.aliasName
        val aliasWriteName = entityDefinitionExtended.writeAliasName
        val indexInfo =
            reactiveElasticSearchOperations.execute { it.indices().getIndex(GetIndexRequest(indexName)) }.awaitFirst()
        assertThat(indexInfo.aliases.values.first().first().alias()).isEqualTo(aliasName)
        assertThat(indexInfo.aliases.values.first()[1].alias).isEqualTo(aliasWriteName)
        assertThat(indexInfo.aliases.values.first().size).isEqualTo(2)
        val currentIndexMapping =
            esMetadataRepository.findById(EsEntityMetadataType.MAPPING.getId(entityDefinitionExtended))?.content
        assertThat(currentIndexMapping).isEqualTo(entityDefinitionExtended.mapping)
        val currentIndexSettings =
            esMetadataRepository.findById(EsEntityMetadataType.SETTINGS.getId(entityDefinitionExtended))?.content
        assertThat(currentIndexSettings).isEqualTo(entityDefinitionExtended.settings)
        val currentIndexVersion =
            esMetadataRepository.findById(EsEntityMetadataType.VERSION_DATA.getId(entityDefinitionExtended))?.content
        assertThat(currentIndexVersion).isEqualTo(EsActivity.VERSION.toString())

        coVerify {
            reindexSchedulingService.scheduleReindex(any(), any())
        }

        coVerify(exactly = 0) {
            reindexSchedulingService.checkReindexInProgress(any())
            reindexSchedulingService.stopTasksIfExists(any())
        }
    }

    @Test
    fun `should update only mappings`() = runBlocking<Unit> {
        bootstrapper.bootstrap()
        clearMocks(reindexSchedulingService)


        val entityDefinitionNew = EntityDefinition(
            entityDefinitionExtended.entity,
            loadMapping,
            entityDefinitionExtended.versionData,
            entityDefinitionExtended.settings
        )
        val bootstrapperNew = ElasticsearchBootstrapper(
            esNameResolver = esNameResolver,
            esOperations = reactiveElasticSearchOperations,
            entityDefinitions = listOf(entityDefinitionNew),
            reindexSchedulingService,
            indexService,
            repositories = emptyList(),
            highLevelClient
        )
        bootstrapperNew.bootstrap()

        val realName = EsHelper.getRealName(
            reactiveElasticSearchOperations, entityDefinitionExtended.writeAliasName, entityDefinitionExtended
        )
        val mapping = getMapping(reactiveElasticSearchOperations, realName!!)
        assertTrue(mapping?.contains("changedFieldForTest") ?: false)

        coVerify(exactly = 0) {
            reindexSchedulingService.checkReindexInProgress(any())
            reindexSchedulingService.scheduleReindex(any(), any())
            reindexSchedulingService.stopTasksIfExists(any())
        }
    }

    @Test
    fun `should update only mappings with reindex old index`() = runBlocking<Unit> {
        bootstrapper.bootstrap()
        clearMocks(reindexSchedulingService)

        val entityDefinitionNew = EntityDefinition(
            entityDefinitionExtended.entity,
            loadMapping,
            newVersionData,
            entityDefinitionExtended.settings
        )
        val bootstrapperNew = ElasticsearchBootstrapper(
            esNameResolver = esNameResolver,
            esOperations = reactiveElasticSearchOperations,
            entityDefinitions = listOf(entityDefinitionNew),
            reindexSchedulingService,
            indexService,
            repositories = emptyList(),
            highLevelClient
        )
        bootstrapperNew.bootstrap()

        val realName = EsHelper.getRealName(
            reactiveElasticSearchOperations, entityDefinitionExtended.writeAliasName, entityDefinitionExtended
        )
        val mapping = getMapping(reactiveElasticSearchOperations, realName!!)
        assertTrue(mapping?.contains("changedFieldForTest") ?: false)

        coVerify {
            reindexSchedulingService.checkReindexInProgress(any())
            reindexSchedulingService.stopTasksIfExists(any())
            reindexSchedulingService.scheduleReindex(any(), any())
        }
    }

    @Test
    fun `update index version`() = runBlocking<Unit> {

        bootstrapper.bootstrap()
        clearMocks(reindexSchedulingService)

        newBootstrapper.bootstrap()

        val currentIndexMapping =
            esMetadataRepository.findById(EsEntityMetadataType.MAPPING.getId(newEntityDefinitionExtended))?.content
        assertThat(currentIndexMapping).isEqualTo(newEntityDefinitionExtended.mapping)
        val currentIndexSettings =
            esMetadataRepository.findById(EsEntityMetadataType.SETTINGS.getId(newEntityDefinitionExtended))?.content
        assertThat(currentIndexSettings).isEqualTo(newEntityDefinitionExtended.settings)
        val currentIndexVersion =
            esMetadataRepository.findById(EsEntityMetadataType.VERSION_DATA.getId(newEntityDefinitionExtended))?.content
        assertThat(currentIndexVersion).isEqualTo(newVersionData.toString())

        val oldIndexName = entityDefinitionExtended.indexName(entityDefinitionExtended.versionData)
        val aliasName = entityDefinitionExtended.aliasName
        val aliasWriteName = entityDefinitionExtended.writeAliasName
        val indexInfo = reactiveElasticSearchOperations.execute { it.indices().getIndex(GetIndexRequest(oldIndexName)) }
            .awaitFirst()
        assertThat(indexInfo.aliases.values.first().size).isEqualTo(2)
        assertThat(indexInfo.aliases.values.first().first().alias()).isEqualTo(aliasName)
        assertThat(indexInfo.aliases.values.first()[1].alias).isEqualTo(aliasWriteName)

        val newIndexName = entityDefinitionExtended.indexName(newVersionData)
        val newIndexInfo =
            reactiveElasticSearchOperations.execute { it.indices().getIndex(GetIndexRequest(newIndexName)) }
                .awaitFirst()

        assertThat(newIndexInfo.aliases.values.first().size).isEqualTo(1)
        assertThat(newIndexInfo.aliases.values.first().first().alias()).isEqualTo(aliasWriteName)
        coVerify {
            reindexSchedulingService.checkReindexInProgress(any())
            reindexSchedulingService.stopTasksIfExists(any())
            reindexSchedulingService.scheduleReindex(any(), any())
        }
    }

    @Test
    fun `update index version with broken process`() = runBlocking<Unit> {

        bootstrapper.bootstrap()
        val newIndexName = entityDefinitionExtended.indexName(newVersionData)
        EsHelper.createIndex(
            reactiveElasticSearchOperations = reactiveElasticSearchOperations,
            name = newIndexName,
            mapping = newEntityDefinition.mapping,
            settings = newEntityDefinition.settings
        )
        EsHelper.createAlias(
            reactiveElasticSearchOperations = reactiveElasticSearchOperations,
            indexName = newIndexName,
            alias = newEntityDefinitionExtended.writeAliasName
        )

        newBootstrapper.bootstrap()

        val currentIndexMapping =
            esMetadataRepository.findById(EsEntityMetadataType.MAPPING.getId(newEntityDefinitionExtended))?.content
        assertThat(currentIndexMapping).isEqualTo(newEntityDefinitionExtended.mapping)
        val currentIndexSettings =
            esMetadataRepository.findById(EsEntityMetadataType.SETTINGS.getId(newEntityDefinitionExtended))?.content
        assertThat(currentIndexSettings).isEqualTo(newEntityDefinitionExtended.settings)
        val currentIndexVersion =
            esMetadataRepository.findById(EsEntityMetadataType.VERSION_DATA.getId(newEntityDefinitionExtended))?.content
        assertThat(currentIndexVersion).isEqualTo(newVersionData.toString())

        val oldIndexName = entityDefinitionExtended.indexName(entityDefinitionExtended.versionData)
        val aliasName = entityDefinitionExtended.aliasName
        val aliasWriteName = entityDefinitionExtended.writeAliasName
        val indexInfo = reactiveElasticSearchOperations.execute { it.indices().getIndex(GetIndexRequest(oldIndexName)) }
            .awaitFirst()
        assertThat(indexInfo.aliases.values.first().size).isEqualTo(2)
        assertThat(indexInfo.aliases.values.first().first().alias()).isEqualTo(aliasName)
        assertThat(indexInfo.aliases.values.first()[1].alias).isEqualTo(aliasWriteName)

        val newIndexInfo =
            reactiveElasticSearchOperations.execute { it.indices().getIndex(GetIndexRequest(newIndexName)) }
                .awaitFirst()

        assertThat(newIndexInfo.aliases.values.first().size).isEqualTo(1)
        assertThat(newIndexInfo.aliases.values.first().first().alias()).isEqualTo(aliasWriteName)

    }
}