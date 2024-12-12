package com.ramitsuri.notificationjournal.core.log

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InMemoryLogWriter : LogWriter() {
    private val _logs = MutableStateFlow<List<LogData>>(listOf())
    val logs = _logs.asStateFlow()

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        _logs.update {
            it + LogData(
                message = message,
                tag = tag,
                errorMessage = throwable?.message,
                stackTrace = throwable?.stackTraceToString(),
            )
        }
        if (_logs.value.size >= MAX_LOGS) {
            val existing = _logs.value
            _logs.update { existing.drop(existing.size - MAX_LOGS) }
        }
    }

    private companion object {
        const val MAX_LOGS = 100
    }
}
