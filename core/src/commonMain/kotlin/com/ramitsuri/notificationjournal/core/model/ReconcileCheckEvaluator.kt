package com.ramitsuri.notificationjournal.core.model

/** Stateless evaluator for [ReconcileCheck] rules against a [DayGroup]. */
object ReconcileCheckEvaluator {
    /**
     * Evaluates [checks] against [dayGroup] and returns the failing checks in input order.
     * Disabled checks and checks not scheduled for [dayGroup]'s day of week are skipped.
     */
    fun evaluate(
        dayGroup: DayGroup,
        checks: List<ReconcileCheck>,
    ): List<ReconcileCheck> =
        checks.filter { check ->
            if (!check.enabled || dayGroup.date.dayOfWeek !in check.daysOfWeek) {
                return@filter false
            }
            val entriesForTag =
                dayGroup.tagGroups
                    .flatMap { it.entries }
                    .filter { it.tag == check.tag }
            val passed =
                when (check.matchType) {
                    ReconcileCheckMatchType.ANY_ENTRY -> entriesForTag.isNotEmpty()
                    ReconcileCheckMatchType.STARTS_WITH -> {
                        val prefix = check.query.trim()
                        prefix.isNotEmpty() &&
                            entriesForTag.any { entry ->
                                entry.text.trim().startsWith(prefix, ignoreCase = true)
                            }
                    }

                    ReconcileCheckMatchType.CONTAINS_ALL_WORDS -> {
                        val wordRegexes =
                            check.queryWords().map { word ->
                                Regex(
                                    pattern = "(?<![\\p{L}\\p{N}])${Regex.escape(word)}(?![\\p{L}\\p{N}])",
                                    option = RegexOption.IGNORE_CASE,
                                )
                            }
                        wordRegexes.isNotEmpty() &&
                            entriesForTag.any { entry ->
                                wordRegexes.all { regex -> regex.containsMatchIn(entry.text) }
                            }
                    }
                }
            !passed
        }
}
