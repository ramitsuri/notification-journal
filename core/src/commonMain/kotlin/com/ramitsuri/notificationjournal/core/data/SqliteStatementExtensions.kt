package com.ramitsuri.notificationjournal.core.data

import androidx.sqlite.SQLiteStatement

fun SQLiteStatement.getColumnIndex(columnName: String): Int = getColumnNames().indexOf(columnName)

fun SQLiteStatement.getLongOrNull(columnIndex: Int): Long? =
    if (isNull(columnIndex)) {
        null
    } else {
        getLong(columnIndex)
    }
