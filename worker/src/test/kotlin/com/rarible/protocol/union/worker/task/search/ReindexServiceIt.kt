package com.rarible.protocol.union.worker.task.search

import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinition
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.repository.search.EsOrderRepository
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.worker.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
internal class ReindexServiceIt {

    @Autowired
    lateinit var taskRepository: TaskRepository

    @Autowired
    lateinit var reindexService: ReindexService

    @Autowired
    lateinit var esActivityRepository: EsActivityRepository

    @Autowired
    lateinit var esCollectionRepository: EsCollectionRepository

    @Autowired
    lateinit var esItemRepository: EsItemRepository

    @Autowired
    lateinit var esOwnershipRepository: EsOwnershipRepository

    @Autowired
    lateinit var esOrderRepository: EsOrderRepository

    @Autowired
    private lateinit var esNameResolver: EsNameResolver

    private val newVersionData = EsActivity.VERSION + 1

    private lateinit var newEntityDefinitionExtendedByNewVersion: EntityDefinitionExtended
    private lateinit var newEntityDefinitionExtendedBySettings: EntityDefinitionExtended

    @BeforeEach
    fun init() {
        val newEntityDefinition = EntityDefinition(
            esCollectionRepository.entityDefinition.entity,
            esCollectionRepository.entityDefinition.mapping,
            newVersionData,
            esCollectionRepository.entityDefinition.settings
        )
        newEntityDefinitionExtendedByNewVersion = esNameResolver.createEntityDefinitionExtended(newEntityDefinition)

        val newEntityDefinitionBySettings = EntityDefinition(
            esCollectionRepository.entityDefinition.entity,
            esCollectionRepository.entityDefinition.mapping,
            esCollectionRepository.entityDefinition.versionData,
            "{}"
        )
        newEntityDefinitionExtendedBySettings =
            esNameResolver.createEntityDefinitionExtended(newEntityDefinitionBySettings)
    }

    @Test
    fun `should schedule activity reindex and alias switch`() = runBlocking<Unit> {
        taskRepository.deleteAll().awaitSingleOrNull()
        reindexService.scheduleReindex("test_activity_index", esActivityRepository.entityDefinition)

        val tasks = taskRepository.findAll().collectList().awaitFirstOrDefault(emptyList())
        Assertions.assertThat(tasks).hasSize(71) //all blockchains * all activities + index switch (minus immutablex)
    }

    @Test
    fun `should schedule collection reindex and alias switch`() = runBlocking<Unit> {
        taskRepository.deleteAll().awaitSingleOrNull()
        reindexService.scheduleReindex("test_collection_index", esCollectionRepository.entityDefinition)

        val tasks = taskRepository.findAll().collectList().awaitFirstOrDefault(emptyList())
        Assertions.assertThat(tasks).hasSize(6) //all blockchains + index switch (minus immutablex)
    }

    @Test
    fun `should schedule ownership reindex and alias switch`() = runBlocking<Unit> {
        taskRepository.deleteAll().awaitSingleOrNull()
        reindexService.scheduleReindex("test_ownership_index", esOwnershipRepository.entityDefinition)

        val tasks = taskRepository.findAll().collectList().awaitFirstOrDefault(emptyList())
        Assertions.assertThat(tasks).hasSize(11) //all enabled blockchains(5) * target.types(2) + index switch(1)
    }

    @Test
    fun `should stop tasks`() = runBlocking<Unit> {
        taskRepository.deleteAll().awaitSingleOrNull()
        reindexService.scheduleReindex("test_collection_index", esCollectionRepository.entityDefinition)
        reindexService.scheduleReindex("test_ownership_index", esOwnershipRepository.entityDefinition)
        val runningTasks = taskRepository.findAll().collectList().awaitFirstOrDefault(emptyList())
        Assertions.assertThat(runningTasks).hasSize(17)
        //collection - all blockchains + index switch (minus immutablex) = 6
        //ownerships - all enabled blockchains(5) * target.types(2) + index switch(1) = 11

        reindexService.stopTasksIfExists(esCollectionRepository.entityDefinition)

        val tasks = taskRepository.findAll().collectList().awaitFirstOrDefault(emptyList())
        Assertions.assertThat(tasks).hasSize(11)
    }

    @Test
    fun `should check reindex in progress`() = runBlocking<Unit> {
        taskRepository.deleteAll().awaitSingleOrNull()
        reindexService.scheduleReindex("test_collection_index", esCollectionRepository.entityDefinition)
        reindexService.scheduleReindex("test_ownership_index", esOwnershipRepository.entityDefinition)

        val progress = reindexService.checkReindexInProgress(esCollectionRepository.entityDefinition)
        assertTrue(progress)
        val progressByNewVersionDefinition =
            reindexService.checkReindexInProgress(newEntityDefinitionExtendedByNewVersion)
        assertFalse(progressByNewVersionDefinition)
        val progressByNewSettings = reindexService.checkReindexInProgress(newEntityDefinitionExtendedBySettings)
        assertFalse(progressByNewSettings)
    }

    @Test
    fun `should check reindex in progress case 2`() = runBlocking<Unit> {
        taskRepository.deleteAll().awaitSingleOrNull()
        reindexService.scheduleReindex("test_collection_index", newEntityDefinitionExtendedBySettings)
        reindexService.scheduleReindex("test_ownership_index", esOwnershipRepository.entityDefinition)

        val progressByNewVersionDefinition =
            reindexService.checkReindexInProgress(newEntityDefinitionExtendedByNewVersion)
        assertFalse(progressByNewVersionDefinition)
        val progressByNewSettings = reindexService.checkReindexInProgress(newEntityDefinitionExtendedBySettings)
        assertTrue(progressByNewSettings)
    }

    @Test
    fun `should check reindex in progress case 3`() = runBlocking<Unit> {
        taskRepository.deleteAll().awaitSingleOrNull()
        reindexService.scheduleReindex("test_collection_index", newEntityDefinitionExtendedByNewVersion)
        reindexService.scheduleReindex("test_ownership_index", esOwnershipRepository.entityDefinition)

        val progressByNewVersionDefinition =
            reindexService.checkReindexInProgress(newEntityDefinitionExtendedByNewVersion)
        assertTrue(progressByNewVersionDefinition)
        val progressByNewSettings = reindexService.checkReindexInProgress(newEntityDefinitionExtendedBySettings)
        assertFalse(progressByNewSettings)
    }
}