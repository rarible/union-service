package com.rarible.protocol.union.worker.task.search.item

import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.worker.task.search.ChangeEsAliasTask
import com.rarible.protocol.union.worker.task.search.ParamFactory
import org.springframework.stereotype.Component

@Component
class ChangeEsItemAliasTask(
    taskRepository: TaskRepository,
    esItemRepository: EsItemRepository,
    indexService: IndexService,
    paramFactory: ParamFactory
) : ChangeEsAliasTask(
    esItemRepository.entityDefinition,
    taskRepository,
    esItemRepository,
    indexService,
    paramFactory
)