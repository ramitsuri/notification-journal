package com.ramitsuri.notificationjournal.core.model

enum class SortOrder(val key: Int) {
    ASC(0),
    DESC(1),
    ;

    companion object {
        fun fromKey(key: Int): SortOrder {
            return values().firstOrNull { it.key == key } ?: ASC
        }
    }
}
