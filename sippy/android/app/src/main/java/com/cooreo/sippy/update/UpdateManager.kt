package com.cooreo.sippy.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.cooreo.sippy.BuildConfig
import com.cooreo.sippy.data.SippyRepository
import com.cooreo.sippy.domain.updates.ApkUrlPolicy
import com.cooreo.sippy.domain.updates.UpdateDecision
import com.cooreo.sippy.domain.updates.UpdateManifest
import com.cooreo.sippy.domain.updates.UpdateManifestParser
import com.cooreo.sippy.domain.updates.UpdatePolicy
import com.cooreo.sippy.util.Hashing
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * The in-app updater.
 *
 * Rules it enforces before anything reaches the system installer:
 *  - HTTPS only, and the apkUrl must live under the project's releases prefix;
 *  - versionCode strictly higher than the installed build;
 *  - SHA-256 matches the manifest;
 *  - the APK's package name matches this build;
 *  - the APK's signing certificate matches the installed app — if it does not,
 *    nothing is installed and the user is told a one-time uninstall is needed
 *    (and that local data will be lost).
 */
class UpdateManager(
    private val context: Context,
    private val repository: SippyRepository,
) {

    data class Progress(val downloading: Boolean = false, val bytes: Long = 0L, val total: Long = 0L)

    private val _progress = MutableStateFlow(Progress())
    val progress: StateFlow<Progress> = _progress.asStateFlow()

    sealed interface Verify {
        data object Ok : Verify
        data class Failed(val reason: String) : Verify
        /** Signature differs from the installed app: only a reinstall can fix it. */
        data object SigningMismatch : Verify
    }

    suspend fun checkForUpdate(manual: Boolean): UpdateDecision = withContext(Dispatchers.IO) {
        val state = repository.state.value
        val now = System.currentTimeMillis()
        if (!manual && !UpdatePolicy.shouldCheckAutomatically(state.update.lastCheckEpochMs, now)) {
            return@withContext UpdateDecision.Skipped
        }
        val manifest = try {
            fetchManifest()
        } catch (t: Throwable) {
            repository.logUpdate("check failed: ${t.message ?: t::class.java.simpleName}")
            repository.edit { it.copy(update = it.update.copy(lastCheckEpochMs = now)) }
            return@withContext UpdateDecision.NoUpdate
        }
        if (manifest == null) {
            repository.logUpdate("check failed: unreadable manifest")
            repository.edit { it.copy(update = it.update.copy(lastCheckEpochMs = now)) }
            return@withContext UpdateDecision.NoUpdate
        }
        repository.logUpdate("checked: remote ${manifest.versionName} (${manifest.versionCode}), local ${BuildConfig.VERSION_CODE}")
        val decision = UpdatePolicy.decide(
            manifest = manifest,
            currentVersionCode = BuildConfig.VERSION_CODE,
            sdkInt = Build.VERSION.SDK_INT,
            snoozedUntilEpochMs = state.update.snoozedUntilEpochMs,
            nowEpochMs = now,
            manual = manual,
            lastCheckEpochMs = 0L,
        )
        repository.edit { it.copy(update = it.update.copy(lastCheckEpochMs = now)) }
        decision
    }

    private fun fetchManifest(): UpdateManifest? {
        val url = URL(MANIFEST_URL)
        if (url.protocol != "https") return null
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = UpdatePolicy.TIMEOUT_MS
            readTimeout = UpdatePolicy.TIMEOUT_MS
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            UpdateManifestParser.parse(text)
        } finally {
            connection.disconnect()
        }
    }

    suspend fun download(manifest: UpdateManifest): Result<File> = withContext(Dispatchers.IO) {
        if (!ApkUrlPolicy.isAllowed(manifest.apkUrl)) {
            return@withContext Result.failure(IllegalArgumentException("apkUrl is not allowed"))
        }
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        // Clean up anything left from an earlier attempt.
        dir.listFiles()?.forEach { if (it.isFile && it.extension != "apk") it.delete() }
        val target = File(dir, "sippy-${manifest.versionName.ifBlank { "update" }}.apk")
        target.delete()

        val url = URL(manifest.apkUrl)
        if (url.protocol != "https") return@withContext Result.failure(IllegalArgumentException("not https"))
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = UpdatePolicy.TIMEOUT_MS
            readTimeout = 30_000
            instanceFollowRedirects = true
        }
        try {
            if (connection.responseCode !in 200..299) {
                return@withContext Result.failure(IllegalStateException("HTTP ${connection.responseCode}"))
            }
            val total = connection.contentLengthLong.coerceAtLeast(manifest.sizeBytes)
            _progress.value = Progress(true, 0, total)
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var written = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        written += read
                        _progress.value = Progress(true, written, total)
                    }
                }
            }
            Result.success(target)
        } catch (t: Throwable) {
            repository.logUpdate("download failed: ${t.message ?: t::class.java.simpleName}")
            target.delete()
            Result.failure(t)
        } finally {
            _progress.value = Progress(false)
            connection.disconnect()
        }
    }

    /** SHA-256 + package name + signing certificate, in that order. */
    fun verify(file: File, manifest: UpdateManifest): Verify {
        val actual = try {
            Hashing.sha256Hex(file)
        } catch (t: Throwable) {
            return Verify.Failed("could not read the download")
        }
        if (!Hashing.matches(manifest.sha256, actual)) {
            return Verify.Failed("checksum mismatch")
        }
        val archive = packageManager().getPackageArchiveInfo(file.absolutePath, 0)
            ?: return Verify.Failed("not an APK")
        if (archive.packageName != context.packageName) {
            return Verify.Failed("package name mismatch")
        }
        val cert = archiveCertSha256(file) ?: return Verify.Failed("no signing certificate")
        val installed = installedCertSha256() ?: return Verify.Failed("could not read the installed certificate")
        if (!Hashing.matches(installed, cert)) return Verify.SigningMismatch
        return Verify.Ok
    }

    @Suppress("DEPRECATION")
    private fun archiveCertSha256(file: File): String? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        val info = packageManager().getPackageArchiveInfo(file.absolutePath, flags) ?: return null
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners
        } else {
            info.signatures
        }
        val first = signatures?.firstOrNull() ?: return null
        return Hashing.sha256Hex(first.toByteArray())
    }

    @Suppress("DEPRECATION")
    private fun installedCertSha256(): String? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        return try {
            val info = packageManager().getPackageInfo(context.packageName, flags)
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.signingInfo?.apkContentsSigners
            } else {
                info.signatures
            }
            signatures?.firstOrNull()?.let { Hashing.sha256Hex(it.toByteArray()) }
        } catch (t: Throwable) {
            null
        }
    }

    fun canInstallPackages(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    fun openUnknownSourcesSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /** Hands the verified APK to the system installer (Android always confirms). */
    fun install(file: File): Boolean = try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (t: Throwable) {
        false
    }

    private fun packageManager() = context.packageManager

    companion object {
        const val MANIFEST_URL =
            "https://raw.githubusercontent.com/Cooreo/SippyDippy/main/releases/latest.json"
    }
}
