package com.ramitsuri.notificationjournal.core.utils

import com.ramitsuri.notificationjournal.core.model.WorkProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class WorkProfileManager(
    private val keyValueStore: KeyValueStoreV2,
) {
    fun getProfileFlow(): Flow<WorkProfile?> =
        combine(
            keyValueStore.getStringFlow(Key.WORK_HOST, ""),
            keyValueStore.getStringFlow(Key.WORK_SALT, ""),
            keyValueStore.getStringFlow(Key.WORK_PASSWORD, ""),
        ) { rawHost, salt, password ->
            mapToWorkProfile(rawHost, salt, password)
        }.distinctUntilChanged()

    suspend fun getProfile(): WorkProfile? {
        val rawHost = keyValueStore.getString(Key.WORK_HOST, "")
        val salt = keyValueStore.getString(Key.WORK_SALT, "")
        val password = keyValueStore.getString(Key.WORK_PASSWORD, "")
        return mapToWorkProfile(rawHost, salt, password)
    }

    suspend fun hasProfile(): Boolean = getProfile() != null

    suspend fun setProfile(profile: WorkProfile) {
        val (cleanHost, port) = WorkProfile.parseHostAndPort(profile.host)
        val finalPort = if (profile.host.contains(":")) port else profile.port
        keyValueStore.putString(Key.WORK_HOST, "$cleanHost:$finalPort")
        keyValueStore.putString(Key.WORK_SALT, profile.salt)
        keyValueStore.putString(Key.WORK_PASSWORD, profile.password)
    }

    suspend fun clearProfile() {
        keyValueStore.putString(Key.WORK_HOST, "")
        keyValueStore.putString(Key.WORK_SALT, "")
        keyValueStore.putString(Key.WORK_PASSWORD, "")
    }

    private fun mapToWorkProfile(
        rawHost: String?,
        salt: String?,
        password: String?,
    ): WorkProfile? {
        if (rawHost.isNullOrBlank() || salt.isNullOrBlank() || password.isNullOrBlank()) {
            return null
        }
        val (host, port) = WorkProfile.parseHostAndPort(rawHost)
        val profile =
            WorkProfile(
                host = host,
                port = port,
                salt = salt,
                password = password,
            )
        return if (profile.isValid()) profile else null
    }
}
