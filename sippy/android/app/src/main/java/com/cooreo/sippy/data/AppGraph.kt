package com.cooreo.sippy.data

import android.content.Context
import com.cooreo.sippy.update.UpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency wiring (no DI framework in v1). Created lazily by
 * [com.cooreo.sippy.SippyApp]; nothing here does I/O at construction time.
 */
class AppGraph(context: Context) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val repository: SippyRepository = DataStoreSippyRepository(context.applicationContext, appScope)
    val updateManager: UpdateManager = UpdateManager(context.applicationContext, repository)
}
