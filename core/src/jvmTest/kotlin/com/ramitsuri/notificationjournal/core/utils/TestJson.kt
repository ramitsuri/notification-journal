package com.ramitsuri.notificationjournal.core.utils

import kotlinx.serialization.json.Json

val testJson =
    Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }
