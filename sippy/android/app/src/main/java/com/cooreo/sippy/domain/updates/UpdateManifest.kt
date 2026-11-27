package com.cooreo.sippy.domain.updates

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * releases/latest.json — the updater manifest.
 *
 * Unknown fields are ignored everywhere, so the manifest can grow without
 * breaking older builds. Every field has a default for the same reason.
 */
@Serializable
data class UpdateManifest(
    val versionCode: Int = 0,
    val versionName: String = "",
    val apkUrl: String = "",
    val sha256: String = "",
    val sizeBytes: Long = 0L,
    val minSdk: Int = 26,
    val releasedAt: String = "",
    val notes: List<String> = emptyList(),
)

object UpdateManifestParser {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    /** Null for anything that is not a usable manifest — never throws. */
    fun parse(raw: String?): UpdateManifest? {
        if (raw.isNullOrBlank()) return null
        return try {
            json.decodeFromString(UpdateManifest.serializer(), raw)
        } catch (t: Throwable) {
            null
        }
    }
}

object ApkUrlPolicy {
    /** The only host/path prefix the updater will ever download from. */
    const val ALLOWED_PREFIX = "https://github.com/Cooreo/SippyDippy/releases/download/"

    /** Case-insensitive prefix check, so a differently-cased URL still works. */
    fun isAllowed(url: String): Boolean =
        url.lowercase().startsWith(ALLOWED_PREFIX.lowercase()) &&
            !url.contains(" ") &&
            !url.contains("\n")
}

object VersionPolicy {
    /** A remote build is offered only when its versionCode is strictly higher. */
    fun isNewer(remoteVersionCode: Int, localVersionCode: Int): Boolean =
        remoteVersionCode > localVersionCode
}

/** What the launch-time / manual update check decided to do. */
sealed interface UpdateDecision {
    data object NoUpdate : UpdateDecision
    /** A newer build exists (or the user asked explicitly) — show the sheet. */
    data class Offer(val manifest: UpdateManifest, val fromManualCheck: Boolean) : UpdateDecision
    /** Nothing to do: checked too recently. */
    data object Skipped : UpdateDecision
}

object UpdatePolicy {
    const val CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L
    const val SNOOZE_MS = 3L * 24L * 60L * 60L * 1000L
    const val TIMEOUT_MS = 8_000

    fun shouldCheckAutomatically(lastCheckEpochMs: Long, nowEpochMs: Long): Boolean =
        nowEpochMs - lastCheckEpochMs >= CHECK_INTERVAL_MS || lastCheckEpochMs <= 0L

    /**
     * @param currentVersionCode the installed build
     * @param sdkInt the device API level (must meet the manifest's minSdk)
     */
    fun decide(
        manifest: UpdateManifest,
        currentVersionCode: Int,
        sdkInt: Int,
        snoozedUntilEpochMs: Long,
        nowEpochMs: Long,
        manual: Boolean,
        lastCheckEpochMs: Long = 0L,
    ): UpdateDecision {
        if (!manual && !shouldCheckAutomatically(lastCheckEpochMs, nowEpochMs)) {
            return UpdateDecision.Skipped
        }
        val usable = VersionPolicy.isNewer(manifest.versionCode, currentVersionCode) &&
            ApkUrlPolicy.isAllowed(manifest.apkUrl) &&
            sdkInt >= manifest.minSdk
        if (!usable) return UpdateDecision.NoUpdate
        if (!manual && nowEpochMs < snoozedUntilEpochMs) return UpdateDecision.Skipped
        return UpdateDecision.Offer(manifest, manual)
    }
}
