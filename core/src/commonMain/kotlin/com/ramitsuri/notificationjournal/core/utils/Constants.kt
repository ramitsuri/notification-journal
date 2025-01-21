package com.ramitsuri.notificationjournal.core.utils

import kotlinx.datetime.LocalDate

object Constants {
    const val REMOTE_INPUT_JOURNAL_KEY = "remote_input_journal_text"
    const val ACTION_JOURNAL = "action_journal"
    const val ACTION_UPLOAD = "action_upload"
    const val TEMPLATED_TIME = "{{time}}"
    const val TEMPLATED_TASK = "[ ]"

    const val PREF_FILE = "com.ramitsuri.notificationjournal.prefs"
    const val PREF_KEY_DATA_HOST = "data_host"
    const val PREF_KEY_DEVICE_NAME = "device_name"
    const val PREF_KEY_DEVICE_ID = "device_id"
    const val PREF_KEY_EXCHANGE_NAME = "exchange_name"
    const val PREF_KEY_USERNAME = "username"
    const val PREF_KEY_PASSWORD = "password"
    const val PREF_WINDOW_SIZE_HEIGHT = "window_size_height"
    const val PREF_WINDOW_SIZE_WIDTH = "window_size_width"
    const val PREF_WINDOW_POSITION_X = "window_position_x"
    const val PREF_WINDOW_POSITION_Y = "window_position_y"

    object WearDataSharing {
        const val JOURNAL_ENTRY_ROUTE = "/journal"
        const val JOURNAL_ENTRY_VALUE = "journal_entry_value"
        const val JOURNAL_ENTRY_TIME = "journal_entry_time"
        const val JOURNAL_ENTRY_TAG = "journal_entry_tag"
        const val REQUEST_UPLOAD_ROUTE = "/upload"
        const val TEMPLATE_ROUTE = "/template"
        const val CLEAR_TEMPLATES_ROUTE = "/clear-templates"
        const val UPDATE_TILE_ROUTE = "/update-tile"
        const val TEMPLATE_ID = "template_id"
        const val TEMPLATE_VALUE = "template_value"
        const val TEMPLATE_TAG = "template_tag"
        const val TEMPLATE_DISPLAY_TEXT = "template_display_text"
        const val TEMPLATE_SHORT_DISPLAY_TEXT = "template_short_display_text"
    }

    object LocalDate {
        val IMPORT_MIN = LocalDate(year = 2020, monthNumber = 1, dayOfMonth = 1)
    }
}