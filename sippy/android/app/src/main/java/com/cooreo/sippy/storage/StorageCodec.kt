package com.cooreo.sippy.storage

import com.cooreo.sippy.model.StoredState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Pure JSON <-> [StoredState] conversion. No Android types, so every recovery
 * path is unit tested on the JVM (first-run bug #1: never crash on load).
 */
object StorageCodec {

    const val SCHEMA_VERSION = 1

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        explicitNulls = false
    }

    sealed interface Decoded {
        data class Ok(val state: StoredState) : Decoded
        /** Unreadable payload; the caller backs the raw text up and starts clean. */
        data class Corrupt(val reason: String) : Decoded
        /** Readable, but written by a schema this build does not understand. */
        data class UnknownVersion(val found: Int) : Decoded
    }

    fun encode(state: StoredState): String =
        json.encodeToString(StoredState.serializer(), state.copy(schemaVersion = SCHEMA_VERSION))

    fun decode(raw: String?): Decoded {
        if (raw.isNullOrBlank()) return Decoded.Ok(StoredState())
        val version = readVersion(raw)
            ?: return Decoded.Corrupt("root is not a JSON object with a schemaVersion")
        if (version != SCHEMA_VERSION) return Decoded.UnknownVersion(version)
        return try {
            val state = json.decodeFromString(StoredState.serializer(), raw)
            // Defensive: a document may claim the current version and still be
            // missing everything; defaults make that a valid clean state.
            Decoded.Ok(state.copy(schemaVersion = SCHEMA_VERSION))
        } catch (t: Throwable) {
            Decoded.Corrupt(t.message ?: t::class.java.simpleName)
        }
    }

    private fun readVersion(raw: String): Int? = try {
        val root = json.parseToJsonElement(raw) as? JsonObject ?: return null
        root["schemaVersion"]?.jsonPrimitive?.intOrNull ?: return null
    } catch (t: Throwable) {
        null
    }

    /** File name used when a document is backed up instead of being loaded. */
    fun backupFileName(nowEpochMs: Long, reason: String): String {
        val safeReason = reason.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(40)
        return "state-$nowEpochMs-${safeReason.ifEmpty { "recovery" }}.json"
    }
}
