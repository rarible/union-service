package com.rarible.protocol.union.worker.task.search.item

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended
import com.rarible.protocol.union.core.model.elasticsearch.EsEntity
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.worker.task.search.ChangeAliasTaskParam
import com.rarible.protocol.union.worker.task.search.ItemTaskParam
import com.rarible.protocol.union.worker.task.search.ParamFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

internal class ChangeEsItemAliasTaskUnitTest {

    private val paramFactory = ParamFactory(jacksonObjectMapper())

    private val completedTask = mockk<Task> {
        every { lastStatus } returns TaskStatus.COMPLETED
    }

    private val failedTask = mockk<Task> {
        every { lastStatus } returns TaskStatus.ERROR
    }
    private val reindexEthereum = ItemTaskParam(
        versionData = 1, settingsHash = "", BlockchainDto.ETHEREUM, "test_item")
    private val reindexFlow = ItemTaskParam(
        versionData = 1, settingsHash = "", BlockchainDto.FLOW, "test_item")

    private val switchAlias = ChangeAliasTaskParam(
        "test_item",
        listOf(
            paramFactory.toString(reindexEthereum),
            paramFactory.toString(reindexFlow)
        )
    )
    private val entityDefinitionExtended = mockk<EntityDefinitionExtended> {
        every {
            reindexTask
        } returns EsItem.ENTITY_DEFINITION.reindexTask

        every {
            entity
        }  returns EsEntity.ITEM
    }

    private val esItemRepository = mockk<EsItemRepository> {

        every {
            entityDefinition
        } returns entityDefinitionExtended

        coEvery {
            refresh()
        } returns Unit
    }

    private val indexService = mockk<IndexService> {
        coEvery {
            finishIndexing(any(), any())
        } returns Unit
    }

    @Test
    fun `should be able to run`() = runBlocking<Unit> {
        val taskRepository = mockk<TaskRepository> {
            every {
                findByTypeAndParam("ITEM_REINDEX", any())
            } returns Mono.just(completedTask)
        }

        Assertions.assertThat(
            ChangeEsItemAliasTask(
                taskRepository, esItemRepository, indexService, paramFactory
            ).isAbleToRun(paramFactory.toString(switchAlias))
        ).isTrue()
    }

    @Test
    fun `should not be able to run`() = runBlocking<Unit> {
        val taskRepository = mockk<TaskRepository> {
            every {
                findByTypeAndParam("ITEM_REINDEX", any())
            } returns Mono.just(failedTask)
        }

        Assertions.assertThat(
            ChangeEsItemAliasTask(
                taskRepository, esItemRepository, indexService, paramFactory
            ).isAbleToRun(paramFactory.toString(switchAlias))
        ).isFalse()
    }

    @Test
    fun `should finish reindexing and switch alias`() = runBlocking {

        val taskRepository = mockk<TaskRepository> {
            every {
                findByTypeAndParam("ITEM_REINDEX", any())
            } returns Mono.just(failedTask)
        }

        val newIndexName = "new_index"
        ChangeEsItemAliasTask(
            taskRepository, esItemRepository, indexService, paramFactory
        ).runLongTask(from = null, param = paramFactory.toString(ChangeAliasTaskParam(newIndexName, listOf())))
            .toList()

        coVerify {
            indexService.finishIndexing(newIndexName, entityDefinitionExtended)
            esItemRepository.refresh()
        }
    }
}