package com.ramitsuri.notificationjournal.core.ui.components

data class JournalEntryDayConfig(
    val allowAdd: Boolean,
    val allowReconcile: Boolean,
    val allowTagMenu: Boolean,
    val allowEdits: Boolean,
    val allowDaySelection: Boolean,
    val allowUpload: Boolean,
) {
    companion object {
        val allEnabled =
            JournalEntryDayConfig(
                allowAdd = true,
                allowReconcile = true,
                allowTagMenu = true,
                allowEdits = true,
                allowDaySelection = true,
                allowUpload = true,
            )
        val allDisabled =
            JournalEntryDayConfig(
                allowAdd = false,
                allowReconcile = false,
                allowTagMenu = false,
                allowEdits = false,
                allowDaySelection = false,
                allowUpload = false,
            )
    }
}
