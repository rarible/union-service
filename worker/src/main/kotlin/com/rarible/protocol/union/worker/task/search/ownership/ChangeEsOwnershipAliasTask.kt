package com.rarible.protocol.union.worker.task.search.ownership

import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.worker.task.search.ChangeEsAliasTask
import com.rarible.protocol.union.worker.task.search.ParamFactory
import org.springframework.stereotype.Component

@Component
class ChangeEsOwnershipAliasTask(
    taskRepository: TaskRepository,
    esOwnershipRepository: EsOwnershipRepository,
    indexService: IndexService,
    paramFactory: ParamFactory
) : ChangeEsAliasTask(
    esOwnershipRepository.entityDefinition,
    taskRepository,
    esOwnershipRepository,
    indexService,
    paramFactory
)