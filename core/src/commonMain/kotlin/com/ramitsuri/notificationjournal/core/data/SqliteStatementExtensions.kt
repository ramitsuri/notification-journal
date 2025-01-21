package com.ramitsuri.notificationjournal.core.data

import androidx.sqlite.SQLiteStatement

fun SQLiteStatement.getColumnIndex(columnName: String): Int {
    return getColumnNames().indexOf(columnName)
}

fun SQLiteStatement.getLongOrNull(columnIndex: Int): Long? {
    return if (isNull(columnIndex)) {
        null
    } else {
        getLong(columnIndex)
    }
}
