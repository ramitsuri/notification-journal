package com.ramitsuri.notificationjournal.core.utils

object Constants {
    const val REMOTE_INPUT_JOURNAL_KEY = "remote_input_journal_text"
    const val ACTION_JOURNAL = "action_journal"
    const val ACTION_UPLOAD = "action_upload"

    const val PREF_FILE = "com.ramitsuri.notificationjournal.prefs"
    const val PREF_KEY_API_URL = "api_url"
    const val PREF_KEY_SORT_ORDER = "sort_order"
    const val PREF_SORT_BY_ENTRY_TIME = "sort_by_entry_time"
    const val PREF_SORT_BY_TAG_ORDER = "sort_by_tag_order"

    const val DEFAULT_API_URL = "http://test.com"

    object DataSharing {
        const val JOURNAL_ENTRY_ROUTE = "/journal"
        const val JOURNAL_ENTRY_VALUE = "journal_entry_value"
        const val JOURNAL_ENTRY_TIME = "journal_entry_time"
        const val JOURNAL_ENTRY_TIME_ZONE = "journal_entry_time_zone"
        const val REQUEST_UPLOAD_ROUTE = "/upload"
    }
}