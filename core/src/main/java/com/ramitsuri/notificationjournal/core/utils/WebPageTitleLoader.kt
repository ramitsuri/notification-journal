package com.ramitsuri.notificationjournal.core.utils

import android.util.Log
import android.webkit.URLUtil
import org.jsoup.Jsoup

fun loadTitle(logTag: String, url: String?): String? {
    if (url == null) {
        Log.d(logTag, "receivedText null")
        return null
    }
    if (!(URLUtil.isHttpUrl(url) || URLUtil.isHttpsUrl(url))) {
        Log.d(logTag, "receivedText not url")
        return null
    }
    try {
        val response = Jsoup.connect(url)
            .ignoreContentType(true)
            .userAgent("Mozilla")
            .referrer("http://www.google.com")
            .timeout(10000)
            .followRedirects(true)
            .execute()

        val doc = response.parse()

        val ogTags = doc.select("meta[property^=og:]")
        if (ogTags.isEmpty()) {
            Log.d(logTag, "Tags empty")
            return null
        }
        ogTags.forEach { tag ->
            when (tag.attr("property")) {
                "og:title" -> {
                    val pageTitle = tag.attr("content")
                    Log.d(logTag, "Page title: $pageTitle")
                    return pageTitle
                }

                else -> {
                    // Do nothing
                }
            }
        }
    } catch (e: Exception) {
        Log.d(logTag, e.message.toString())
    }
    return null
}