package com.ramitsuri.notificationjournal.ai.extractor

import co.touchlab.kermit.Logger
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.ramitsuri.notificationjournal.ai.model.AddEntryIntent
import com.ramitsuri.notificationjournal.ai.model.ExtractionResult
import com.ramitsuri.notificationjournal.ai.prompt.JournalPrompts
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.PrefManager
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

private const val TAG = "JournalAI_LiteRT"

class LiteRtLmAddEntryIntentExtractor(
    private val modelFile: File,
    private val backend: Backend = Backend.CPU(),
    private val cacheDir: File? = null,
    private val prefManager: PrefManager? = null,
    private val fallbackExtractor: AddEntryIntentExtractor = RuleBasedAddEntryIntentExtractor(),
) : AddEntryIntentExtractor {
    private val executor = Executors.newSingleThreadExecutor()
    private val dispatcher = executor.asCoroutineDispatcher()

    private var engine: Engine? = null
    private var isInitialized = false

    override suspend fun isAvailable(): Boolean =
        withContext(dispatcher) {
            modelFile.exists() && modelFile.isFile && modelFile.length() > 0
        }

    private fun ensureInitialized() {
        if (isInitialized && engine != null) return
        val config =
            EngineConfig(
                modelPath = modelFile.absolutePath,
                backend = backend,
                cacheDir = cacheDir?.absolutePath,
            )
        val newEngine = Engine(config)
        newEngine.initialize()
        engine = newEngine
        isInitialized = true
        Logger.withTag(TAG).i { "Initialized LiteRT-LM engine with ${modelFile.name} (backend=$backend)" }
    }

    override suspend fun extractIntent(userInput: String): ExtractionResult<AddEntryIntent>? =
        withContext(dispatcher) {
            val customRules = prefManager?.getCustomPromptRules()

            var fallbackReason = "Unknown"
            var fallbackThrowable: Throwable? = null
            var fallbackRawResponse: String? = null

            try {
                if (!modelFile.exists() || !modelFile.isFile || modelFile.length() == 0L) {
                    throw IllegalStateException("Model file does not exist at ${modelFile.absolutePath}")
                }

                ensureInitialized()
                val currentEngine = checkNotNull(engine) { "Engine not initialized" }

                val prompt = JournalPrompts.getAddEntryPrompt(userInput, customRules = customRules)
                Logger.withTag(TAG).d { "Running LiteRT-LM inference for: $userInput" }

                val conversationConfig =
                    ConversationConfig(
                        samplerConfig =
                            SamplerConfig(
                                topK = 1,
                                topP = 0.95,
                                temperature = 0.0,
                            ),
                    )

                val conversation = currentEngine.createConversation(conversationConfig)
                val rawResponse =
                    try {
                        conversation.sendMessage(prompt).toString()
                    } finally {
                        try {
                            conversation.close()
                        } catch (e: Exception) {
                            Logger.withTag(TAG).w(e) { "Error closing conversation" }
                        }
                    }

                Logger.withTag(TAG).d { "LiteRT-LM raw response: $rawResponse" }
                fallbackRawResponse = rawResponse

                if (rawResponse.isNotBlank()) {
                    val parsed = IntentJsonParser.parseJson(rawResponse, userInput)
                    if (parsed != null) {
                        Logger.withTag(TAG).i { "LiteRT-LM parsing succeeded: $parsed" }
                        return@withContext ExtractionResult(
                            intent = parsed,
                            engine = "LiteRT-LM (${modelFile.nameWithoutExtension})",
                        )
                    } else {
                        fallbackReason = "Could not parse JSON from LiteRT-LM response"
                    }
                } else {
                    fallbackReason = "LiteRT-LM returned empty or blank response"
                }
            } catch (e: Exception) {
                Logger.withTag(TAG).e(e) { "LiteRT-LM inference failed" }
                fallbackReason = "LiteRT-LM inference failed: ${e.message}"
                fallbackThrowable = e
            }

            val logMessage =
                buildString {
                    append("Reason: ").append(fallbackReason)
                    append("\nModel: ").append(modelFile.name)
                    append("\nInput: \"").append(userInput).append("\"")
                    if (fallbackRawResponse != null) {
                        append("\nRaw Response: ").append(fallbackRawResponse)
                    }
                }
            Logger.withTag(Constants.TAG_AI_FALLBACK).w(fallbackThrowable) { logMessage }

            Logger.withTag(TAG).d { "Using fallback extractor for: $userInput" }
            fallbackExtractor.extractIntent(userInput)
        }

    override fun release() {
        executor.execute {
            try {
                engine?.close()
            } catch (e: Exception) {
                Logger.withTag(TAG).w(e) { "Error closing LiteRT-LM engine" }
            } finally {
                engine = null
                isInitialized = false
            }
            executor.shutdown()
        }
        fallbackExtractor.release()
    }
}
