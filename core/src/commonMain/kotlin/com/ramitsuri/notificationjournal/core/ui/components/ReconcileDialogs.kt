package com.ramitsuri.notificationjournal.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import notificationjournal.core.generated.resources.Res
import notificationjournal.core.generated.resources.cancel
import notificationjournal.core.generated.resources.ok
import notificationjournal.core.generated.resources.reconcile_blocker_conflicts
import notificationjournal.core.generated.resources.reconcile_blocker_intro
import notificationjournal.core.generated.resources.reconcile_blocker_title
import notificationjournal.core.generated.resources.reconcile_blocker_untagged_entries
import notificationjournal.core.generated.resources.reconcile_checks_failed_intro
import notificationjournal.core.generated.resources.reconcile_checks_failed_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReconcileBlockerDialog(
    untaggedCount: Int,
    conflictCount: Int,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(Res.string.reconcile_blocker_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(Res.string.reconcile_blocker_intro))
                if (untaggedCount > 0) {
                    Text(
                        text =
                            pluralStringResource(
                                Res.plurals.reconcile_blocker_untagged_entries,
                                untaggedCount,
                                untaggedCount,
                            ),
                    )
                }
                if (conflictCount > 0) {
                    Text(
                        text =
                            pluralStringResource(
                                Res.plurals.reconcile_blocker_conflicts,
                                conflictCount,
                                conflictCount,
                            ),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.ok))
            }
        },
    )
}

@Composable
fun ReconcileChecksWarningDialog(
    confirmButtonText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(Res.string.reconcile_checks_failed_title))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = stringResource(Res.string.reconcile_checks_failed_intro))
                content()
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}
