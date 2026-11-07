package com.ramitsuri.notificationjournal.core.log

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import com.ramitsuri.notificationjournal.core.data.LogDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.time.Clock

class DatabaseLogWriter(
    private val logDao: LogDao,
    private val coroutineScope: CoroutineScope,
) : LogWriter() {
    val logs: Flow<List<LogData>> = logDao.getLogsFlow()

    private var insertCount = 0

    override fun log(
        severity: Severity,
        message: String,
        tag: String,
        throwable: Throwable?,
    ) {
        coroutineScope.launch {
            try {
                logDao.insert(
                    LogData(
                        time = Clock.System.now(),
                        severity = severity,
                        message = message,
                        tag = tag,
                        errorMessage = throwable?.message,
                        stackTrace = throwable?.stackTraceToString(),
                    ),
                )
                if (++insertCount % PRUNE_FREQUENCY == 0) {
                    logDao.pruneOldLogs(MAX_LOGS)
                }
            } catch (_: Exception) {
                // Ignore failures to avoid recursive logging loops
            }
        }
    }

    fun clear() {
        coroutineScope.launch {
            try {
                logDao.deleteAll()
            } catch (_: Exception) {
            }
        }
    }

    fun getTags(): Flow<List<String>> = logDao.getTagsFlow()

    companion object {
        const val MAX_LOGS = 2000
        private const val PRUNE_FREQUENCY = 50
    }
}
