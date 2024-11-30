package com.ramitsuri.notificationjournal.core.data

import androidx.sqlite.SQLiteStatement

fun SQLiteStatement.getTextOrNull(columnIndex: Int): String? {
    return if (isNull(columnIndex)) {
        null
    } else {
        getText(columnIndex)
    }
}
