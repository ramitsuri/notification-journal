package com.ramitsuri.notificationjournal.core.utils

import com.ramitsuri.notificationjournal.core.di.Factory

interface KeyValueStore {
    fun getString(key: String, fallback: String): String?
    fun putString(key: String, value: String)

    fun getInt(key: String, fallback: Int): Int
    fun putInt(key: String, value: Int)

    fun getBoolean(key: String, fallback: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)

    fun hasKey(key: String): Boolean
}

class PrefsKeyValueStore(factory: Factory) : KeyValueStore {
    private val prefs = factory.getSettings()

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

    override fun getBoolean(key: String, fallback: Boolean): Boolean {
        return try {
            prefs.getBoolean(key, fallback)
        } catch (e: ClassCastException) {
            fallback
        }
    }

    override fun putBoolean(key: String, value: Boolean) {
        prefs.putBoolean(key, value)
    }

    override fun hasKey(key: String): Boolean {
        return prefs.hasKey(key)
    }
}