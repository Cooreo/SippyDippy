package com.cooreo.sippy.data

import com.cooreo.sippy.model.StoredState
import kotlinx.coroutines.flow.StateFlow

/**
 * The one persistence seam. v1 is DataStore + kotlinx.serialization; Room can
 * replace it later behind this interface without touching the UI.
 *
 * Every call is total: a corrupt or unknown-version document is backed up and
 * replaced by a clean state, never surfaced as an exception.
 */
interface SippyRepository {
    val state: StateFlow<StoredState>

    /** False until the first read finished; the UI shows the splash until then. */
    val ready: StateFlow<Boolean>

    /** Atomically read-modify-write the document. */
    suspend fun edit(transform: (StoredState) -> StoredState)

    suspend fun resetToDefault()

    /** Appends one updater log line, keeping only the newest 20. */
    suspend fun logUpdate(line: String)

    /** Files written to the backups folder (developer screen). */
    fun backupFiles(): List<String>
}
