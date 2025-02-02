package com.ramitsuri.notificationjournal.core.utils

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toOkioPath
import java.nio.file.Paths
import java.util.UUID

fun getTestDataKeyValueStore(): KeyValueStoreV2 {
    val dataStore =
        PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                Paths.get(Constants.BASE_DIR).resolve("${UUID.randomUUID()}.preferences_pb").toOkioPath()
            },
        )
    return DataStoreKeyValueStore(dataStore)
}
