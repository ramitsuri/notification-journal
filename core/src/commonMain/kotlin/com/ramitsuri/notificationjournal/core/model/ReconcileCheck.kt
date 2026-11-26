package com.ramitsuri.notificationjournal.core.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.DayOfWeek
import java.util.UUID

/**
 * User-configured expectation evaluated against a [DayGroup] when reconciling.
 */
@Entity(tableName = "reconcile_checks")
data class ReconcileCheck(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "tag")
    val tag: String,
    @ColumnInfo(name = "days_of_week")
    val daysOfWeek: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    @ColumnInfo(name = "match_type")
    val matchType: ReconcileCheckMatchType,
    @ColumnInfo(name = "query")
    val query: String = "",
    @ColumnInfo(name = "enabled", defaultValue = "1")
    val enabled: Boolean = true,
) {
    /** Returns a concise human-readable description of this check for Settings and warning dialogs. */
    fun summary(): String {
        val daysSummary =
            if (daysOfWeek.size == DayOfWeek.entries.size) {
                "Every day"
            } else {
                DayOfWeek.entries
                    .filter { it in daysOfWeek }
                    .joinToString(", ") { it.shortName() }
            }
        val conditionSummary =
            when (matchType) {
                ReconcileCheckMatchType.ANY_ENTRY -> "at least 1 entry"
                ReconcileCheckMatchType.STARTS_WITH -> "starts with \"${query.trim()}\""
                ReconcileCheckMatchType.CONTAINS_ALL_WORDS -> {
                    val words = queryWords().joinToString(", ") { "\"$it\"" }
                    "contains $words"
                }
            }
        return "$daysSummary • $tag • $conditionSummary"
    }

    internal fun queryWords(): List<String> =
        buildList {
            val token = StringBuilder()
            for (ch in query) {
                if (ch == ',' || ch.isWhitespace()) {
                    if (token.isNotEmpty()) {
                        add(token.toString())
                        token.clear()
                    }
                } else {
                    token.append(ch)
                }
            }
            if (token.isNotEmpty()) {
                add(token.toString())
            }
        }
}

private fun DayOfWeek.shortName(): String =
    when (this) {
        DayOfWeek.MONDAY -> "Mon"
        DayOfWeek.TUESDAY -> "Tue"
        DayOfWeek.WEDNESDAY -> "Wed"
        DayOfWeek.THURSDAY -> "Thu"
        DayOfWeek.FRIDAY -> "Fri"
        DayOfWeek.SATURDAY -> "Sat"
        DayOfWeek.SUNDAY -> "Sun"
    }
