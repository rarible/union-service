package com.rarible.protocol.union.enrichment.meta.downloader.executor

import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.core.model.download.DownloadException
import com.rarible.protocol.union.core.model.download.DownloadStatus
import com.rarible.protocol.union.core.model.download.DownloadTask
import com.rarible.protocol.union.enrichment.configuration.DownloaderProperties
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadEntryRepository
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadNotifier
import com.rarible.protocol.union.enrichment.meta.downloader.Downloader
import com.rarible.protocol.union.enrichment.util.optimisticLockWithInitial
import kotlinx.coroutines.awaitAll
import org.slf4j.LoggerFactory

/**
 * Async data download executor, end point of entire download pipeline.
 */
class DownloadExecutor<T>(
    private val repository: DownloadEntryRepository<T>,
    private val downloader: Downloader<T>,
    private val debouncer: DownloadDebouncer,
    private val notifier: DownloadNotifier<T>,
    private val pool: DownloadPool,
    properties: DownloaderProperties,
) {

    private val maxRetries = properties.retries.size

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun execute(tasks: List<DownloadTask>) {
        tasks.map {
            pool.submitAsync { execute(it) }
        }.awaitAll()
    }

    private suspend fun execute(task: DownloadTask) {
        val current = getOrDefault(task)
        if (debouncer.debounce(task, current)) {
            // TODO add metrics
            return
        }

        try {
            val data = downloader.download(task.id)
            onSuccess(current, task, data)
        } catch (e: DownloadException) {
            onFail(current, task, e.message)
        } catch (e: Exception) {
            logger.error("Unexpected exception while downloading data for task {}", task.id, e)
            onFail(current, task, e.message)
        }
    }

    private suspend fun onSuccess(
        current: DownloadEntry<T>,
        task: DownloadTask,
        data: T
    ) = optimisticLockWithInitial(current) { initial ->

        // For successful case we should rewrite current data anyway
        val exist = initial ?: getOrDefault(task)
        val updated = exist.withSuccessInc(data)
        repository.save(updated)

        notifier.notify(updated)
        logger.info("Data download SUCCEEDED for: {}", task.id)

    }

    private suspend fun onFail(
        current: DownloadEntry<T>,
        task: DownloadTask,
        errorMessage: String?
    ) = optimisticLockWithInitial(current) { initial ->
        val exist = initial ?: getOrDefault(task)

        val failed = exist.withFailInc(errorMessage)

        val isRetryLimitExceeded = failed.retries >= maxRetries
        val status = if (isRetryLimitExceeded) DownloadStatus.FAILED else DownloadStatus.RETRY

        val updated = when (failed.status) {
            // Nothing to do here, we don't want to replace existing data, just update fail counters
            DownloadStatus.SUCCESS, DownloadStatus.FAILED -> failed
            // Failed on retry, just update status, retry counter should be managed by job
            // Status can be changed here if retry limit exceeded
            DownloadStatus.RETRY -> failed.copy(status = status)
            // That was first download, set retry counter as 0 (never retried before)
            // SCHEDULE can turn into FAILED only if we set retry policy with 0 retries
            DownloadStatus.SCHEDULED -> failed.copy(status = status, retries = 0)
        }

        repository.save(updated)

        logger.warn(
            "Data download FAILED for: {}, status = {}, retries = {}, errorMessage = {}",
            failed.id, failed.status, failed.retries, failed.errorMessage
        )
    }

    private suspend fun getOrDefault(task: DownloadTask): DownloadEntry<T> {
        repository.get(task.id)?.let { return it }

        // This should never happen, originally, at Executor stage entry MUST always exist
        logger.warn("Entry {} not found, using default state", task.id)
        return DownloadEntry(
            id = task.id,
            status = DownloadStatus.SCHEDULED,
            scheduledAt = task.scheduledAt
        )
    }

}