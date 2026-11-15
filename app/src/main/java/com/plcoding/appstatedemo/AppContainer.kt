package com.plcoding.appstatedemo

import android.content.Context
import androidx.appstate.datastore.syncToDataStore
import androidx.appstate.statestore.StateStore
import com.plcoding.appstatedemo.data.AndroidNetworkMonitor
import com.plcoding.appstatedemo.data.NetworkMonitor
import com.plcoding.appstatedemo.data.TaskRepository
import com.plcoding.appstatedemo.focus.FocusSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class AppContainer(context: Context) {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val stateStore = StateStore()

    val taskRepository = TaskRepository()

    val networkMonitor: NetworkMonitor = AndroidNetworkMonitor(context)

    val focusSessionManager = FocusSessionManager(
        stateStore = stateStore,
        scope = applicationScope,
    )

    init {
        val storeFile = File(context.filesDir, "datastore/statestore.json")
        applicationScope.launch {
            stateStore.syncToDataStore(
                path = storeFile.absolutePath,
                coroutineScope = applicationScope,
            )
        }
    }
}
