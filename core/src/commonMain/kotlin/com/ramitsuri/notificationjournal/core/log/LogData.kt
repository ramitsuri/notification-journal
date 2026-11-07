package com.ramitsuri.notificationjournal.core.log

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import co.touchlab.kermit.Severity
import kotlin.time.Clock
import kotlin.time.Instant

@Entity(tableName = "logs")
data class LogData(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "time")
    val time: Instant = Clock.System.now(),
    @ColumnInfo(name = "severity")
    val severity: Severity = Severity.Info,
    @ColumnInfo(name = "message")
    val message: String,
    @ColumnInfo(name = "tag")
    val tag: String,
    @ColumnInfo(name = "errorMessage")
    val errorMessage: String? = null,
    @ColumnInfo(name = "stackTrace")
    val stackTrace: String? = null,
)
