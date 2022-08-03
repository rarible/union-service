package com.rarible.protocol.union.worker.task.search

import com.rarible.core.common.mapAsync
import com.rarible.core.logging.Logger
import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.union.core.elasticsearch.ReindexSchedulingService
import com.rarible.protocol.union.core.elasticsearch.bootstrap.ElasticsearchBootstrapper
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.EsCollection
import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsOrder
import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended
import com.rarible.protocol.union.core.model.elasticsearch.EsEntity
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.worker.config.WorkerProperties
import com.rarible.protocol.union.worker.task.search.ChangeEsAliasTask.Companion.getChangeAliasTaskName
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Component

@Component
class ReindexService(
    // don't remove this unused dep. It used to check create bean on not test env
    private val elasticsearchBootstrapper: ElasticsearchBootstrapper,
    workerProperties: WorkerProperties,
    private val taskRepository: TaskRepository,
    private val paramFactory: ParamFactory,
) : ReindexSchedulingService {

    private val searchReindexProperties = workerProperties.searchReindex

    override suspend fun scheduleReindex(
        newIndexName: String,
        entityDefinition: EntityDefinitionExtended
    ) {
        when (entityDefinition.entity) {
            EsEntity.ACTIVITY -> {
                scheduleActivityReindex(newIndexName, entityDefinition)
            }
            EsEntity.ITEM -> {
                scheduleItemReindex(newIndexName, entityDefinition)
            }
            EsEntity.COLLECTION -> {
                scheduleCollectionReindex(newIndexName, entityDefinition)
            }
            EsEntity.ORDER -> {
                scheduleOrderReindex(newIndexName, entityDefinition)
            }
            EsEntity.OWNERSHIP -> {
                scheduleOwnershipReindex(newIndexName, entityDefinition)
            }
        }
    }

    private suspend fun scheduleCollectionReindex(indexName: String, definition: EntityDefinitionExtended) {
        val blockchains = searchReindexProperties.collection.activeBlockchains()
        val taskParams = blockchains.map {
            paramFactory.toString(
                CollectionTaskParam(
                    versionData = definition.versionData,
                    settingsHash = definition.settingsHash,
                    blockchain = it,
                    index = indexName
                )
            )
        }
        val tasks = tasks(EsCollection.ENTITY_DEFINITION.reindexTask, taskParams)

        val changeAliasTaskParam = ChangeAliasTaskParam(
            indexName, taskParams
        )

        val indexSwitch = indexSwitchTask(
            entityName = EsEntity.COLLECTION.entityName,
            changeAliasTaskParam = changeAliasTaskParam,
            definition
        )

        taskRepository.saveAll(tasks + indexSwitch).collectList().awaitFirst()
    }

    private suspend fun scheduleActivityReindex(indexName: String, definition: EntityDefinitionExtended) {
        val blockchains = searchReindexProperties.activity.activeBlockchains()
        val types = ActivityTypeDto.values()
        val taskParams = blockchains.flatMap { blockchain ->
            types.map { type ->
                paramFactory.toString(
                    ActivityTaskParam(
                        versionData = definition.versionData,
                        settingsHash = definition.settingsHash,
                        blockchain = blockchain,
                        type = type,
                        index = indexName
                    )
                )
            }
        }

        val tasks = tasks(EsActivity.ENTITY_DEFINITION.reindexTask, taskParams)

        val changeAliasTaskParam = ChangeAliasTaskParam(
            indexName, taskParams
        )

        val indexSwitch = indexSwitchTask(
            entityName = EsEntity.ACTIVITY.entityName,
            changeAliasTaskParam = changeAliasTaskParam,
            definition
        )
        taskRepository.saveAll(tasks + indexSwitch).collectList().awaitFirst()
    }

    private suspend fun scheduleItemReindex(indexName: String, definition: EntityDefinitionExtended) {
        val blockchains = searchReindexProperties.item.activeBlockchains()
        val taskParams = blockchains.map {
            paramFactory.toString(
                ItemTaskParam(
                    versionData = definition.versionData,
                    settingsHash = definition.settingsHash,
                    blockchain = it,
                    index = indexName
                )
            )
        }
        val tasks = tasks(EsItem.ENTITY_DEFINITION.reindexTask, taskParams)
        val changeEsItemAliasTaskParam = ChangeAliasTaskParam(
            indexName, taskParams
        )
        val indexSwitch = indexSwitchTask(
            entityName = EsEntity.ITEM.entityName,
            changeAliasTaskParam = changeEsItemAliasTaskParam,
            definition
        )
        taskRepository.saveAll(tasks + indexSwitch).collectList().awaitFirst()
    }

    private suspend fun scheduleOrderReindex(indexName: String, definition: EntityDefinitionExtended) {
        val blockchains = searchReindexProperties.order.activeBlockchains()
        val taskParams = blockchains.map {
            paramFactory.toString(
                OrderTaskParam(
                    versionData = definition.versionData,
                    settingsHash = definition.settingsHash,
                    blockchain = it,
                    index = indexName
                )
            )
        }
        val tasks = tasks(EsOrder.ENTITY_DEFINITION.reindexTask, taskParams)
        val changeEsOrderAliasTaskParam = ChangeAliasTaskParam(
            indexName, taskParams
        )
        val indexSwitch = indexSwitchTask(
            entityName = EsEntity.ORDER.entityName,
            changeAliasTaskParam = changeEsOrderAliasTaskParam,
            definition
        )
        taskRepository.saveAll(tasks + indexSwitch).collectList().awaitFirst()
    }

    private suspend fun scheduleOwnershipReindex(indexName: String, definition: EntityDefinitionExtended) {
        val blockchains = searchReindexProperties.ownership.activeBlockchains()
        val taskParams = blockchains.flatMap { blockchain ->
            OwnershipTaskParam.Target.values().map { target ->
                paramFactory.toString(
                    OwnershipTaskParam(
                        versionData = definition.versionData,
                        settingsHash = definition.settingsHash,
                        blockchain = blockchain,
                        index = indexName,
                        target = target
                    )
                )
            }
        }
        val tasks = tasks(EsOwnership.ENTITY_DEFINITION.reindexTask, taskParams)
        val changeEsOwnershipAliasTaskParam = ChangeAliasTaskParam(
            indexName, taskParams
        )
        val indexSwitch = indexSwitchTask(
            entityName = EsEntity.OWNERSHIP.entityName,
            changeAliasTaskParam = changeEsOwnershipAliasTaskParam,
            definition
        )
        taskRepository.saveAll(tasks + indexSwitch).collectList().awaitFirst()
    }

    private suspend fun tasks(reindexTask: String, params: List<String>): List<Task> =
        params.mapAsync {
            task(taskType = reindexTask, taskParamJson = it)
        }.filterNotNull()

    private suspend fun task(
        taskType: String,
        taskParamJson: String
    ): Task? {

        val existing = taskRepository
            .findByTypeAndParam(taskType, taskParamJson)
            .awaitSingleOrNull()
        return if (existing == null) {
            logger.info(
                "Scheduling activity reindexing with param: {}",
                taskParamJson
            )
            Task(
                type = taskType,
                param = taskParamJson,
                state = null,
                running = false,
                lastStatus = TaskStatus.NONE
            )
        } else {
            logger.info(
                "Activity reindexing with param {} already exists with id={}",
                taskParamJson, existing.id.toHexString()
            )
            null
        }
    }

    private suspend fun indexSwitchTask(
        entityName: String,
        changeAliasTaskParam: ChangeAliasTaskParam,
        definition: EntityDefinitionExtended
    ): List<Task> {

        val taskParamJson = paramFactory.toString(changeAliasTaskParam)
        val taskType = definition.getChangeAliasTaskName()
        val existing = taskRepository
            .findByTypeAndParam(taskType, taskParamJson)
            .awaitSingleOrNull()
        return if (existing == null) {
            logger.info(
                "Scheduling {} index alias switch with param: {}",
                entityName,
                changeAliasTaskParam
            )
            listOf(
                Task(
                    type = taskType,
                    param = taskParamJson,
                    state = null,
                    running = false,
                    lastStatus = TaskStatus.NONE
                )
            )
        } else {
            logger.info(
                "{} index alias switch with param {} already exists with id={}",
                entityName, changeAliasTaskParam, existing.id.toHexString()
            )
            emptyList()
        }
    }

    override suspend fun stopTasksIfExists(entityDefinition: EntityDefinitionExtended) {
        val tasks = taskRepository.findAll()
            .filter { entityDefinition.reindexTask == it.type || entityDefinition.getChangeAliasTaskName() == it.type }
            .collectList()
            .awaitFirst()

        taskRepository.deleteAll(tasks).awaitFirstOrNull()
    }

    override suspend fun checkReindexInProgress(
        entityDefinition: EntityDefinitionExtended
    ): Boolean {
        val tasks = taskRepository.findAll()
            .filter {
                if (entityDefinition.reindexTask == it.type) {
                    val taskParam = paramFactory.parse<RawTaskParam>(it.param)
                    entityDefinition.versionData == taskParam.versionData &&
                        entityDefinition.settingsHash == taskParam.settingsHash
                } else false
            }
            .collectList()
            .awaitFirst()

        return tasks.isNotEmpty()
    }

    companion object {
        val logger by Logger()
    }
}