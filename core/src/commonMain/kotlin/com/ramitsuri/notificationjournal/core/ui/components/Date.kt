package com.ramitsuri.notificationjournal.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.reset
import notificationjournal.core.generated.resources.today
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@Composable
fun Date(
    selectedDate: LocalDate? = null,
    allowedSelections: ClosedRange<LocalDate>? = null,
    onDateSelected: ((LocalDate) -> Unit)? = null,
    onResetDate: () -> Unit,
    onResetDateToToday: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
            ),
    ) {
        Card(modifier = Modifier.padding(16.dp)) {
            fun Long.toLocalDate(): LocalDate {
                return Instant
                    .fromEpochMilliseconds(this)
                    .toLocalDateTime(TimeZone.UTC)
                    .date
            }

            fun LocalDate.toMillisSinceEpoch(): Long {
                return this
                    .atStartOfDayIn(TimeZone.UTC)
                    .toEpochMilliseconds()
            }

            val yearRange =
                if (allowedSelections == null) {
                    DatePickerDefaults.YearRange
                } else {
                    allowedSelections.start.year..allowedSelections.endInclusive.year
                }
            val selectedDateMillis = remember(selectedDate) { selectedDate?.toMillisSinceEpoch() }
            val initialDisplayedMonthMillis =
                selectedDateMillis ?: allowedSelections?.endInclusive?.toMillisSinceEpoch()
            val state =
                rememberDatePickerState(
                    initialSelectedDateMillis = selectedDateMillis,
                    initialDisplayedMonthMillis = initialDisplayedMonthMillis,
                    yearRange = yearRange,
                    selectableDates =
                        object : SelectableDates {
                            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                if (allowedSelections == null) {
                                    return true
                                }
                                val pickedDate = utcTimeMillis.toLocalDate()
                                return allowedSelections.contains(pickedDate)
                            }

                            override fun isSelectableYear(year: Int): Boolean {
                                if (allowedSelections == null) {
                                    return true
                                }
                                return (allowedSelections.start.year..allowedSelections.endInclusive.year).contains(
                                    year,
                                )
                            }
                        },
                )

            LaunchedEffect(Unit) {
                snapshotFlow { state.selectedDateMillis }.collect { selectedDateMillis ->
                    if (selectedDateMillis != null &&
                        selectedDateMillis != selectedDate?.toMillisSinceEpoch()
                    ) {
                        onDateSelected?.invoke(selectedDateMillis.toLocalDate())
                    }
                }
            }

            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DatePicker(
                    state = state,
                    showModeToggle = false,
                    headline = null,
                    title = null,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = {
                        onResetDate()
                        onDismiss()
                    }) {
                        Text(stringResource(Res.string.reset))
                    }
                    onResetDateToToday?.let {
                        TextButton(onClick = {
                            onResetDateToToday()
                            onDismiss()
                        }) {
                            Text(stringResource(Res.string.today))
                        }
                    }
                }
            }
        }
    }
}
