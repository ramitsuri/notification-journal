package com.ramitsuri.notificationjournal.core.utils

import android.content.Context
import android.content.SharedPreferences

interface KeyValueStore {
    fun getString(key: String, fallback: String?): String?
    fun putString(key: String, value: String?)

    fun getLong(key: String, fallback: Long): Long
    fun putLong(key: String, value: Long)

    fun getInt(key: String, fallback: Int): Int
    fun putInt(key: String, value: Int)

    fun getBoolean(key: String, fallback: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)

    fun getFloat(key: String, fallback: Float): Float
    fun putFloat(key: String, value: Float)

    fun contains(key: String): Boolean
    fun remove(key: String)
    fun removeAll()
}

class PrefsKeyValueStore(context: Context, fileName: String) : KeyValueStore {
    private val prefs =
        context.applicationContext.getSharedPreferences(fileName, Context.MODE_PRIVATE)

    override fun getString(key: String, fallback: String?): String? {
        return try {
            prefs.getString(key, fallback)
        } catch (e: ClassCastException) {
            fallback
        }
    }

    override fun putString(key: String, value: String?) {
        editAndApply {
            putString(key, value)
        }
    }

    override fun getLong(key: String, fallback: Long): Long {
        return try {
            prefs.getLong(key, fallback)
        } catch (e: ClassCastException) {
            fallback
        }
    }

    override fun putLong(key: String, value: Long) {
        editAndApply {
            putLong(key, value)
        }
    }

    override fun getInt(key: String, fallback: Int): Int {
        return try {
            prefs.getInt(key, fallback)
        } catch (e: ClassCastException) {
            fallback
        }
    }

    override fun putInt(key: String, value: Int) {
        editAndApply {
            putInt(key, value)
        }
    }

    override fun getBoolean(key: String, fallback: Boolean): Boolean {
        return try {
            prefs.getBoolean(key, fallback)
        } catch (e: ClassCastException) {
            fallback
        }
    }

    override fun putBoolean(key: String, value: Boolean) {
        editAndApply {
            putBoolean(key, value)
        }
    }

    override fun getFloat(key: String, fallback: Float): Float {
        return try {
            prefs.getFloat(key, fallback)
        } catch (e: ClassCastException) {
            fallback
        }
    }

    override fun putFloat(key: String, value: Float) {
        editAndApply {
            putFloat(key, value)
        }
    }

    override fun contains(key: String): Boolean {
        return prefs.contains(key)
    }

    override fun remove(key: String) {
        editAndApply {
            remove(key)
        }
    }

    override fun removeAll() {
        editAndApply {
            clear()
        }
    }

    private fun editAndApply(block: SharedPreferences.Editor.() -> SharedPreferences.Editor) {
        prefs.edit()
            .apply { block() }
            .apply()
    }

    companion object {
        private const val TAG = "PrefsKeyValueStore"
    }
}