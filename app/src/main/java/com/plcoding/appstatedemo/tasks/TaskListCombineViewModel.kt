package com.plcoding.appstatedemo.tasks

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.snapshotFlow
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
import com.plcoding.appstatedemo.focus.FocusSession
import com.plcoding.appstatedemo.focus.FocusSessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@OptIn(SavedStateHandleSaveableApi::class, FlowPreview::class, ExperimentalCoroutinesApi::class)
class TaskListCombineViewModel(
    private val taskRepository: TaskRepository,
    private val focusSessionManager: FocusSessionManager,
    networkMonitor: NetworkMonitor,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val searchQuery by savedStateHandle.saveable(saver = TextFieldState.Saver) { TextFieldState() }
    private val hideCompleted = MutableStateFlow(false)
    private val openTasksFirst = MutableStateFlow(false)
    private val isLoading = MutableStateFlow(true)

    private val searchResults = snapshotFlow { searchQuery.text.toString() }
        .debounce(300.milliseconds)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            taskRepository.searchTasks(query)
                .onStart { isLoading.value = true }
                .onEach { isLoading.value = false }
        }

    val state: StateFlow<TaskListState> = combine(
        searchResults,
        isLoading,
        hideCompleted,
        openTasksFirst,
        snapshotFlow { focusSessionManager.session.value },
        snapshotFlow { networkMonitor.isOnline.value },
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val tasks = values[0] as List<Task>
        val isLoading = values[1] as Boolean
        val hideCompleted = values[2] as Boolean
        val openTasksFirst = values[3] as Boolean
        val focusSession = values[4] as FocusSession
        val isOnline = values[5] as Boolean

        TaskListState(
            tasks = tasks.applyFilters(hideCompleted, openTasksFirst),
            isLoading = isLoading,
            hideCompleted = hideCompleted,
            openTasksFirst = openTasksFirst,
            focusSession = focusSession,
            isOnline = isOnline,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TaskListState(),
    )

    fun onAction(action: TaskListAction) {
        when (action) {
            is TaskListAction.OnHideCompletedChange -> hideCompleted.value = action.hide
            is TaskListAction.OnOpenTasksFirstChange -> openTasksFirst.value = action.enabled
            TaskListAction.OnResetFiltersClick -> {
                searchQuery.clearText()
                hideCompleted.value = false
                openTasksFirst.value = false
            }
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

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as AppStateDemoApp).container
                TaskListCombineViewModel(
                    taskRepository = container.taskRepository,
                    focusSessionManager = container.focusSessionManager,
                    networkMonitor = container.networkMonitor,
                    savedStateHandle = createSavedStateHandle(),
                )
            }
        }
    }
}
