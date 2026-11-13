package com.ramitsuri.notificationjournal.ai.extractor

enum class SupportedCustomModel(
    val id: String,
    val displayName: String,
    val fileName: String,
    val downloadUrl: String,
) {
    QWEN_2_5_1_5B(
        id = "qwen2.5-1.5b",
        displayName = "Qwen 2.5 1.5B Instruct",
        fileName = "qwen2.5-1.5b-instruct.litertlm",
        downloadUrl =
            "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/" +
                "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm",
    ),
}
