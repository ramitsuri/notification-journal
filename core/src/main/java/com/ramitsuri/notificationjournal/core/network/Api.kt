package com.ramitsuri.notificationjournal.core.network

import com.ramitsuri.notificationjournal.core.data.JournalEntry
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

interface Api {
    @POST("/")
    suspend fun sendData(@Body entries: List<JournalEntry>): Response<ResponseBody>
}

fun <T> buildApi(baseUrl: String, apiClass: Class<T>): T {
    val moshi = Moshi.Builder()
        .add(InstantAdapter())
        .add(ZoneIdAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val httpClientBuilder = OkHttpClient.Builder()
    val logging = HttpLoggingInterceptor()
    logging.setLevel(HttpLoggingInterceptor.Level.BODY)
    httpClientBuilder.addInterceptor(logging)

    val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(httpClientBuilder.build())
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    return retrofit.create(apiClass)
}

private class InstantAdapter() {
    @ToJson
    fun toJson(instant: Instant): String {
        return DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC)).format(instant)
    }

    @FromJson
    fun fromJson(json: String): Instant {
        return Instant.parse(json)
    }
}

private class ZoneIdAdapter() {
    @ToJson
    fun toJson(zoneId: ZoneId): String {
        return zoneId.id
    }

    @FromJson
    fun fromJson(json: String): ZoneId {
        return ZoneId.of(json)
    }
}