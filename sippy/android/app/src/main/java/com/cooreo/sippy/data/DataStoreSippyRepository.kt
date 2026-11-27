package com.cooreo.sippy.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cooreo.sippy.model.StoredState
import com.cooreo.sippy.storage.StorageCodec
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private val Context.sippyStore by preferencesDataStore(name = "sippy")

class DataStoreSippyRepository(
    private val context: Context,
    scope: CoroutineScope,
) : SippyRepository {

    private val key = stringPreferencesKey("state_json")
    private val mutex = Mutex()

    private val _state = MutableStateFlow(StoredState())
    override val state: StateFlow<StoredState> = _state.asStateFlow()

    private val _ready = MutableStateFlow(false)
    override val ready: StateFlow<Boolean> = _ready.asStateFlow()

    init {
        // Load off the main thread; the UI shows the splash until `ready`.
        scope.launch(Dispatchers.IO) { load() }
    }

    private suspend fun load() = withContext(Dispatchers.IO) {
        val raw = try {
            var value: String? = null
            context.sippyStore.edit { prefs -> value = prefs[key] }
            value
        } catch (t: Throwable) {
            null
        }
        when (val decoded = StorageCodec.decode(raw)) {
            is StorageCodec.Decoded.Ok -> _state.value = decoded.state
            is StorageCodec.Decoded.Corrupt -> recover(raw, "corrupt: ${decoded.reason}")
            is StorageCodec.Decoded.UnknownVersion -> recover(raw, "unknown-version-${decoded.found}")
        }
        _ready.value = true
    }

    /** First-run bug #1: back the unreadable document up, then start clean. */
    private suspend fun recover(raw: String?, reason: String) {
        try {
            if (!raw.isNullOrBlank()) {
                val dir = File(context.filesDir, "backups").apply { mkdirs() }
                val name = StorageCodec.backupFileName(System.currentTimeMillis(), reason)
                File(dir, name).writeText(raw)
            }
        } catch (_: Throwable) {
            // A failed backup must not stop the app from starting clean.
        }
        _state.value = StoredState()
        runCatching { context.sippyStore.edit { it.remove(key) } }
    }

    override suspend fun edit(transform: (StoredState) -> StoredState) {
        mutex.withLock {
            val next = transform(_state.value)
            withContext(Dispatchers.IO) {
                runCatching {
                    context.sippyStore.edit { prefs ->
                        prefs[key] = StorageCodec.encode(next)
                    }
                }
            }
            _state.value = next
        }
    }

    override suspend fun resetToDefault() {
        mutex.withLock {
            val fresh = StoredState()
            withContext(Dispatchers.IO) {
                runCatching {
                    context.sippyStore.edit { prefs ->
                        prefs[key] = StorageCodec.encode(fresh)
                    }
                }
            }
            _state.value = fresh
        }
    }

    override suspend fun logUpdate(line: String) {
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        edit { current ->
            val kept = (current.update.log + "$stamp  $line").takeLast(20)
            current.copy(update = current.update.copy(log = kept))
        }
    }

    override fun backupFiles(): List<String> {
        val dir = File(context.filesDir, "backups")
        return dir.listFiles()?.sortedByDescending { it.name }?.map { it.name }.orEmpty()
    }
}
