package com.rarible.protocol.union.worker.task.search.collection

import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.worker.task.search.ChangeEsAliasTask
import com.rarible.protocol.union.worker.task.search.ParamFactory
import org.springframework.stereotype.Component

@Component
class ChangeEsCollectionAliasTask(
    taskRepository: TaskRepository,
    esCollectionRepository: EsCollectionRepository,
    indexService: IndexService,
    paramFactory: ParamFactory
) : ChangeEsAliasTask(
    esCollectionRepository.entityDefinition,
    taskRepository,
    esCollectionRepository,
    indexService,
    paramFactory
)