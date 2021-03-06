package com.rarible.protocol.union.worker.task.search

import com.rarible.core.common.mapAsync
import com.rarible.core.logging.Logger
import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.union.core.elasticsearch.ReindexSchedulingService
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.EsCollection
import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsOrder
import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended
import com.rarible.protocol.union.core.model.elasticsearch.EsEntity
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.worker.config.WorkerProperties
import com.rarible.protocol.union.worker.task.search.activity.ActivityTaskParam
import com.rarible.protocol.union.worker.task.search.activity.ChangeEsActivityAliasTask
import com.rarible.protocol.union.worker.task.search.collection.ChangeEsCollectionAliasTask
import com.rarible.protocol.union.worker.task.search.collection.CollectionTaskParam
import com.rarible.protocol.union.worker.task.search.item.ChangeEsItemAliasTask
import com.rarible.protocol.union.worker.task.search.item.ItemTaskParam
import com.rarible.protocol.union.worker.task.search.order.ChangeEsOrderAliasTask
import com.rarible.protocol.union.worker.task.search.order.OrderTaskParam
import com.rarible.protocol.union.worker.task.search.ownership.ChangeEsOwnershipAliasTask
import com.rarible.protocol.union.worker.task.search.ownership.OwnershipTaskParam
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Component

@Component
class ReindexService(
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
                scheduleActivityReindex(newIndexName)
            }
            EsEntity.ITEM -> {
                scheduleItemReindex(newIndexName)
            }
            EsEntity.COLLECTION -> {
                scheduleCollectionReindex(newIndexName)
            }
            EsEntity.ORDER -> {
                scheduleOrderReindex(newIndexName)
            }
            EsEntity.OWNERSHIP -> {
                scheduleOwnershipReindex(newIndexName)
            }
        }
    }

    suspend fun scheduleCollectionReindex(indexName: String) {
        val blockchains = searchReindexProperties.collection.activeBlockchains()
        val taskParams = blockchains.map {
            paramFactory.toString(CollectionTaskParam(it, indexName))
        }
        val tasks = tasks(EsCollection.ENTITY_DEFINITION.reindexTask, taskParams)

        val changeAliasTaskParam = ChangeAliasTaskParam(
            indexName, taskParams
        )

        val indexSwitch = indexSwitchTask(
            entityName = EsEntity.COLLECTION.entityName,
            changeAliasTaskParam = changeAliasTaskParam,
            taskType = ChangeEsCollectionAliasTask.TYPE
        )

        taskRepository.saveAll(tasks + indexSwitch).collectList().awaitFirst()
    }

    suspend fun scheduleActivityReindex(indexName: String) {
        val blockchains = searchReindexProperties.activity.activeBlockchains()
        val types = ActivityTypeDto.values()
        val taskParams = blockchains.flatMap { blockchain ->
            types.map { type ->
                paramFactory.toString(ActivityTaskParam(blockchain, type, indexName))
            }
        }

        val tasks = tasks(EsActivity.ENTITY_DEFINITION.reindexTask, taskParams)

        val changeAliasTaskParam = ChangeAliasTaskParam(
            indexName, taskParams
        )

        val indexSwitch = indexSwitchTask(
            entityName = EsEntity.ACTIVITY.entityName,
            changeAliasTaskParam = changeAliasTaskParam,
            taskType = ChangeEsActivityAliasTask.TYPE
        )
        taskRepository.saveAll(tasks + indexSwitch).collectList().awaitFirst()
    }

    suspend fun scheduleItemReindex(indexName: String) {
        val blockchains = searchReindexProperties.item.activeBlockchains()
        val taskParams = blockchains.map {
            paramFactory.toString(ItemTaskParam(blockchain = it, index = indexName))
        }
        val tasks = tasks(EsItem.ENTITY_DEFINITION.reindexTask, taskParams)
        val changeEsItemAliasTaskParam = ChangeAliasTaskParam(
            indexName, taskParams
        )
        val indexSwitch = indexSwitchTask(
            entityName = EsEntity.ITEM.entityName,
            changeAliasTaskParam = changeEsItemAliasTaskParam,
            taskType = ChangeEsItemAliasTask.TYPE
        )
        taskRepository.saveAll(tasks + indexSwitch).collectList().awaitFirst()
    }

    suspend fun scheduleOrderReindex(indexName: String) {
        val blockchains = searchReindexProperties.order.activeBlockchains()
        val taskParams = blockchains.map {
            paramFactory.toString(OrderTaskParam(blockchain = it, index = indexName))
        }
        val tasks = tasks(EsOrder.ENTITY_DEFINITION.reindexTask, taskParams)
        val changeEsOrderAliasTaskParam = ChangeAliasTaskParam(
            indexName, taskParams
        )
        val indexSwitch = indexSwitchTask(
            entityName = EsEntity.ORDER.entityName,
            changeAliasTaskParam = changeEsOrderAliasTaskParam,
            taskType = ChangeEsOrderAliasTask.TYPE
        )
        taskRepository.saveAll(tasks + indexSwitch).collectList().awaitFirst()
    }

    suspend fun scheduleOwnershipReindex(indexName: String) {
        val blockchains = searchReindexProperties.ownership.activeBlockchains()
        val taskParams = blockchains.flatMap { blockchain ->
            OwnershipTaskParam.Target.values().map { target ->
                paramFactory.toString(OwnershipTaskParam(blockchain = blockchain, index = indexName, target = target))
            }
        }
        val tasks = tasks(EsOwnership.ENTITY_DEFINITION.reindexTask, taskParams)
        val changeEsOwnershipAliasTaskParam = ChangeAliasTaskParam(
            indexName, taskParams
        )
        val indexSwitch = indexSwitchTask(
            entityName = EsEntity.OWNERSHIP.entityName,
            changeAliasTaskParam = changeEsOwnershipAliasTaskParam,
            taskType = ChangeEsOwnershipAliasTask.TYPE
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

    private suspend fun <T> indexSwitchTask(
        entityName: String,
        changeAliasTaskParam: T,
        taskType: String
    ): List<Task> {

        val taskParamJson = paramFactory.toString(changeAliasTaskParam)

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

    companion object {
        val logger by Logger()
    }
}