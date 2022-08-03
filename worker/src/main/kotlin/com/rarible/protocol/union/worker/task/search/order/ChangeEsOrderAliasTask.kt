package com.rarible.protocol.union.worker.task.search.order

import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.enrichment.repository.search.EsOrderRepository
import com.rarible.protocol.union.worker.task.search.ChangeEsAliasTask
import com.rarible.protocol.union.worker.task.search.ParamFactory
import org.springframework.stereotype.Component

@Component
class ChangeEsOrderAliasTask(
    taskRepository: TaskRepository,
    esOrderRepository: EsOrderRepository,
    indexService: IndexService,
    paramFactory: ParamFactory
) : ChangeEsAliasTask(
    esOrderRepository.entityDefinition,
    taskRepository,
    esOrderRepository,
    indexService,
    paramFactory
)