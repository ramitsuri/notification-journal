package com.ramitsuri.notificationjournal.core.model

data class WorkProfile(
    val host: String,
    val port: Int = DEFAULT_PORT,
    val salt: String,
    val password: String,
) {
    fun isValid(): Boolean = host.isNotBlank() && salt.isNotBlank() && password.isNotBlank() && port > 0

    companion object {
        const val DEFAULT_PORT = 8080

        fun parseHostAndPort(hostWithOptionalPort: String): Pair<String, Int> {
            val host = hostWithOptionalPort.substringBefore(":")
            val port =
                if (hostWithOptionalPort.contains(":")) {
                    hostWithOptionalPort.substringAfter(":").toIntOrNull() ?: DEFAULT_PORT
                } else {
                    DEFAULT_PORT
                }
            return host to port
        }
    }
}
