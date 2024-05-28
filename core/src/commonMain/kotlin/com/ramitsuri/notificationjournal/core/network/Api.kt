package com.ramitsuri.notificationjournal.core.network

import com.ramitsuri.notificationjournal.core.model.DayGroup
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.logging.Logger as KtorLogger

interface Api {
    suspend fun sendData(entries: List<DayGroup>): HttpResponse?
}

internal class ApiImpl(
    private val baseUrl: String,
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : Api {
    override suspend fun sendData(entries: List<DayGroup>): HttpResponse? {
        return withContext(ioDispatcher) {
            try {
                return@withContext httpClient.post("$baseUrl/") {
                    setBody(entries)
                }
            } catch (e: Exception) {
                println("API: Error sending data: $e")
                null
            }
        }
    }
}

fun buildApi(baseUrl: String, isDebug: Boolean): Api {
    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                },
            )
        }
        install(DefaultRequest) {
            contentType(ContentType.Application.Json)
        }
        install(Logging) {
            logger =
                object : KtorLogger {
                    override fun log(message: String) {
                        println("API: $message")
                    }
                }
            level = if (isDebug) LogLevel.ALL else LogLevel.NONE
        }
    }

    return ApiImpl(baseUrl, client)
}