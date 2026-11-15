package com.plcoding.appstatedemo.tasks

import androidx.appstate.transform.transform
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.plcoding.appstatedemo.AppStateDemoApp
import com.plcoding.appstatedemo.data.NetworkMonitor
import com.plcoding.appstatedemo.data.Task
import com.plcoding.appstatedemo.data.TaskRepository
import com.plcoding.appstatedemo.focus.FocusSessionManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@OptIn(SavedStateHandleSaveableApi::class)
class TaskListViewModel(
    private val taskRepository: TaskRepository,
    private val focusSessionManager: FocusSessionManager,
    networkMonitor: NetworkMonitor,
    savedStateHandle: SavedStateHandle,
    coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel(coroutineScope) {

    val searchQuery by savedStateHandle.saveable(saver = TextFieldState.Saver) { TextFieldState() }
    private var hideCompleted by mutableStateOf(false)
    private var openTasksFirst by mutableStateOf(false)
    private var isLoading by mutableStateOf(true)

    private val debouncedSearchQuery by transform(
        initialValue = searchQuery.text.toString(),
        scope = viewModelScope,
        dispatcher = dispatcher,
    ) {
        debounce(300.milliseconds, searchQuery.text.toString())
    }

    private val filteredTasks by transform(
        initialValue = emptyList<Task>(),
        scope = viewModelScope,
        dispatcher = dispatcher,
    ) {
        val query = debouncedSearchQuery
        val searchResults by produceState(initialValue = emptyList(), query) {
            isLoading = true
            taskRepository.searchTasks(query).collect { tasks ->
                value = tasks
                isLoading = false
            }
        }
        searchResults.applyFilters(hideCompleted, openTasksFirst)
    }

    val state: State<TaskListState> = transform(
        initialValue = TaskListState(),
        scope = viewModelScope,
        dispatcher = dispatcher,
    ) {
        TaskListState(
            tasks = filteredTasks,
            isLoading = isLoading,
            hideCompleted = hideCompleted,
            openTasksFirst = openTasksFirst,
            focusSession = focusSessionManager.session.value,
            isOnline = networkMonitor.isOnline.value,
        )
    }

    fun onAction(action: TaskListAction) {
        when (action) {
            is TaskListAction.OnHideCompletedChange -> hideCompleted = action.hide
            is TaskListAction.OnOpenTasksFirstChange -> openTasksFirst = action.enabled
            TaskListAction.OnResetFiltersClick -> resetFilters()
            is TaskListAction.OnTaskCheckedChange -> taskRepository.toggleCompleted(action.task.id)
            is TaskListAction.OnFocusClick -> focusSessionManager.startFocus(
                taskId = action.task.id,
                taskTitle = action.task.title,
                duration = 1.minutes,
            )
            TaskListAction.OnExtendFocusClick -> focusSessionManager.extendFocus(by = 1.minutes)
            TaskListAction.OnStopFocusClick -> focusSessionManager.stopFocus()
        }
    }

    private fun resetFilters() {
        Snapshot.withMutableSnapshot {
            searchQuery.clearText()
            hideCompleted = false
            openTasksFirst = false
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as AppStateDemoApp).container
                TaskListViewModel(
                    taskRepository = container.taskRepository,
                    focusSessionManager = container.focusSessionManager,
                    networkMonitor = container.networkMonitor,
                    savedStateHandle = createSavedStateHandle(),
                )
            }
        }
    }
}

@Composable
private fun <T> debounce(timeout: Duration, value: T): T {
    var debouncedValue by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        delay(timeout)
        debouncedValue = value
    }
    return debouncedValue
}
