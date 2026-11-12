package dev.slne.surf.api.core.server.environment

import dev.slne.surf.api.common.util.logger
import java.io.File

object EnvironmentAccessor {
    private val log = logger()

    private const val DOTENV_FILE_NAME = ".env"

    @Volatile
    private var cachedDotenv: Map<String, String>? = null

    fun getEnv(key: String): String? {
        val dotenv = loadDotenv()
        return dotenv[key]?.takeIf { it.isNotBlank() } ?: System.getenv(key)
            ?.takeIf { it.isNotBlank() }
    }

    fun getMergedEnv(): Map<String, String> {
        val systemEnv = System.getenv()
        val dotenv = loadDotenv()
        return LinkedHashMap(systemEnv).apply { putAll(dotenv) }
    }

    fun reset() {
        synchronized(this) {
            cachedDotenv = null
        }
    }

    private fun loadDotenv(): Map<String, String> {
        cachedDotenv?.let { return it }
        synchronized(this) {
            cachedDotenv?.let { return it }

            val file = File(DOTENV_FILE_NAME)
            val parsed = if (file.exists() && file.isFile) {
                try {
                    parseDotenv(file)
                } catch (e: Exception) {
                    log.atFine()
                        .log("Failed to read .env file: %s", file.absolutePath, e)
                    emptyMap()
                }
            } else {
                emptyMap()
            }

            cachedDotenv = parsed
            return parsed
        }
    }

    private fun parseDotenv(file: File): Map<String, String> {
        val result = LinkedHashMap<String, String>()

        file.readLines(Charsets.UTF_8).forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach

            val idx = line.indexOf('=')
            if (idx <= 0) return@forEach

            val key = line.substring(0, idx).trim()
            var value = line.substring(idx + 1).trim()

            if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith(
                    "'"
                ))
            ) {
                value = value.substring(1, value.length - 1)
            }

            if (key.isNotEmpty()) {
                result[key] = value
            }
        }

        return result
    }
}