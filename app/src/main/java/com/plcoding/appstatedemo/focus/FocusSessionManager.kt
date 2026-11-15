package com.plcoding.appstatedemo.focus

import androidx.appstate.datastore.PersistToDataStore
import androidx.appstate.statestore.StateStore
import androidx.appstate.statestore.StateStoreKey
import androidx.appstate.transform.listener
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Serializable
private data object FocusSessionKey :
    StateStoreKey<FocusSession>(defaultValue = FocusSession.Idle),
    PersistToDataStore

class FocusSessionManager(
    private val stateStore: StateStore,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {

    val session: State<FocusSession> = stateStore.getState(FocusSessionKey)

    init {
        scope.launch {
            listener(dispatcher) {
                val current = session.value
                if (current is FocusSession.Running) {
                    LaunchedEffect(current.endTimeMillis) {
                        val remainingMillis = current.endTimeMillis - currentTimeMillis()
                        if (remainingMillis > 0) {
                            delay(remainingMillis.milliseconds)
                        }
                        stateStore.setState(FocusSessionKey, FocusSession.Idle)
                    }
                }
            }
        }
    }

    fun startFocus(taskId: String, taskTitle: String, duration: Duration) {
        stateStore.setState(
            FocusSessionKey,
            FocusSession.Running(
                taskId = taskId,
                taskTitle = taskTitle,
                endTimeMillis = currentTimeMillis() + duration.inWholeMilliseconds,
            ),
        )
    }

    fun extendFocus(by: Duration) {
        stateStore.updateState(FocusSessionKey) { current ->
            if (current is FocusSession.Running) {
                current.copy(endTimeMillis = current.endTimeMillis + by.inWholeMilliseconds)
            } else {
                current
            }
        }
    }

    fun stopFocus() {
        stateStore.setState(FocusSessionKey, FocusSession.Idle)
    }
}
