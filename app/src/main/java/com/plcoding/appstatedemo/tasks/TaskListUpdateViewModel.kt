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
import com.plcoding.appstatedemo.focus.FocusSessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@OptIn(SavedStateHandleSaveableApi::class, FlowPreview::class, ExperimentalCoroutinesApi::class)
class TaskListUpdateViewModel(
    private val taskRepository: TaskRepository,
    private val focusSessionManager: FocusSessionManager,
    networkMonitor: NetworkMonitor,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val searchQuery by savedStateHandle.saveable(saver = TextFieldState.Saver) { TextFieldState() }

    private val _state = MutableStateFlow(TaskListState())
    val state: StateFlow<TaskListState> = _state.asStateFlow()

    private var searchResults = emptyList<Task>()

    init {
        snapshotFlow { searchQuery.text.toString() }
            .debounce(300.milliseconds)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                taskRepository.searchTasks(query)
                    .onStart { _state.update { it.copy(isLoading = true) } }
            }
            .onEach { tasks ->
                searchResults = tasks
                _state.update {
                    it.copy(
                        tasks = tasks.applyFilters(it.hideCompleted, it.openTasksFirst),
                        isLoading = false,
                    )
                }
            }
            .launchIn(viewModelScope)

        snapshotFlow { focusSessionManager.session.value }
            .onEach { session -> _state.update { it.copy(focusSession = session) } }
            .launchIn(viewModelScope)

        snapshotFlow { networkMonitor.isOnline.value }
            .onEach { isOnline -> _state.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
    }

    fun onAction(action: TaskListAction) {
        when (action) {
            is TaskListAction.OnHideCompletedChange -> _state.update {
                it.copy(
                    hideCompleted = action.hide,
                    tasks = searchResults.applyFilters(action.hide, it.openTasksFirst),
                )
            }
            is TaskListAction.OnOpenTasksFirstChange -> _state.update {
                it.copy(
                    openTasksFirst = action.enabled,
                    tasks = searchResults.applyFilters(it.hideCompleted, action.enabled),
                )
            }
            TaskListAction.OnResetFiltersClick -> {
                searchQuery.clearText()
                _state.update {
                    it.copy(
                        hideCompleted = false,
                        openTasksFirst = false,
                        tasks = searchResults.applyFilters(hideCompleted = false, openTasksFirst = false),
                    )
                }
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
                TaskListUpdateViewModel(
                    taskRepository = container.taskRepository,
                    focusSessionManager = container.focusSessionManager,
                    networkMonitor = container.networkMonitor,
                    savedStateHandle = createSavedStateHandle(),
                )
            }
        }
    }
}
