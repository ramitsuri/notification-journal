package com.ramitsuri.notificationjournal.core.utils

object Constants {
    const val REMOTE_INPUT_JOURNAL_KEY = "remote_input_journal_text"
    const val ACTION_JOURNAL = "action_journal"

    const val PREF_FILE = "com.ramitsuri.notificationjournal.prefs"
    const val PREF_KEY_SORT_ORDER = "sort_order"
    const val PREF_KEY_DATA_HOST = "data_host"
    const val PREF_KEY_DEVICE_NAME = "device_name"
    const val PREF_KEY_DEVICE_ID = "device_id"
    const val PREF_KEY_EXCHANGE_NAME = "exchange_name"
    const val PREF_KEY_USERNAME = "username"
    const val PREF_KEY_PASSWORD = "password"

    object WearDataSharing {
        const val JOURNAL_ENTRY_ROUTE = "/journal"
        const val JOURNAL_ENTRY_VALUE = "journal_entry_value"
        const val JOURNAL_ENTRY_TIME = "journal_entry_time"
        const val JOURNAL_ENTRY_TIME_ZONE = "journal_entry_time_zone"
        const val JOURNAL_ENTRY_TAG = "journal_entry_tag"
        const val REQUEST_UPLOAD_ROUTE = "/upload"
        const val TEMPLATE_ROUTE = "/template"
        const val TEMPLATE_ID = "template_id"
        const val TEMPLATE_VALUE = "template_value"
        const val TEMPLATE_TAG = "template_tag"
    }
}