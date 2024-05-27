package com.ramitsuri.notificationjournal.core.utils

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings

interface KeyValueStore {
    fun getString(key: String, fallback: String): String?
    fun putString(key: String, value: String)

    fun getInt(key: String, fallback: Int): Int
    fun putInt(key: String, value: Int)
}

class PrefsKeyValueStore(context: Context) : KeyValueStore {
    private val prefs = SharedPreferencesSettings(
        context.applicationContext.getSharedPreferences(
            Constants.PREF_FILE,
            Context.MODE_PRIVATE
        )
    )

    override fun getString(key: String, fallback: String): String? {
        return try {
            prefs.getString(key, fallback)
        } catch (e: ClassCastException) {
            fallback
        }
    }

    override fun putString(key: String, value: String) {
        prefs.putString(key, value)
    }

    override fun getInt(key: String, fallback: Int): Int {
        return try {
            prefs.getInt(key, fallback)
        } catch (e: ClassCastException) {
            fallback
        }
    }

    override fun putInt(key: String, value: Int) {
        prefs.putInt(key, value)
    }
}