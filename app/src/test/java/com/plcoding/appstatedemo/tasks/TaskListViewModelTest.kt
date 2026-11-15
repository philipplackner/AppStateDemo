package com.plcoding.appstatedemo.tasks

import androidx.appstate.statestore.StateStore
import androidx.appstate.transform.testing.TransformTestScope
import androidx.appstate.transform.testing.transformTest
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import com.plcoding.appstatedemo.data.NetworkMonitor
import com.plcoding.appstatedemo.data.TaskRepository
import com.plcoding.appstatedemo.focus.FocusSession
import com.plcoding.appstatedemo.focus.FocusSessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModelTest {

    private class FakeNetworkMonitor : NetworkMonitor {
        val online = mutableStateOf(true)
        override val isOnline: State<Boolean> = online
    }

    private fun TransformTestScope.createViewModel(
        networkMonitor: NetworkMonitor = FakeNetworkMonitor(),
    ): TaskListViewModel {
        return TaskListViewModel(
            taskRepository = TaskRepository(),
            focusSessionManager = FocusSessionManager(
                stateStore = StateStore(),
                scope = backgroundScope,
                dispatcher = testDispatcher,
                currentTimeMillis = { testCoroutineScheduler.currentTime },
            ),
            networkMonitor = networkMonitor,
            savedStateHandle = SavedStateHandle(),
            coroutineScope = backgroundScope,
            dispatcher = testDispatcher,
        )
    }

    private fun TaskListViewModel.content(): TaskListState {
        return state.value.also { assertFalse(it.isLoading) }
    }

    @Test
    fun searchIsDebounced() = transformTest {
        val viewModel = createViewModel()
        advance()
        assertEquals(6, viewModel.content().tasks.size)

        viewModel.searchQuery.setTextAndPlaceCursorAtEnd("gradle")
        advance()
        advanceTimeBy(200.milliseconds)
        advance()
        assertEquals(6, viewModel.content().tasks.size)

        advanceTimeBy(100.milliseconds)
        advance()
        advance()
        assertEquals(listOf("Update Gradle dependencies"), viewModel.content().tasks.map { it.title })
    }

    @Test
    fun searchWithoutResultsIsNotLoading() = transformTest {
        val viewModel = createViewModel()
        assertTrue(viewModel.state.value.isLoading)
        advance()

        viewModel.searchQuery.setTextAndPlaceCursorAtEnd("no such task")
        advance()
        advanceTimeBy(300.milliseconds)
        advance()
        advance()
        advance()
        assertEquals(emptyList<Any>(), viewModel.content().tasks)
    }

    @Test
    fun hideCompletedFiltersWithoutReloading() = transformTest {
        val viewModel = createViewModel()
        advance()

        viewModel.onAction(TaskListAction.OnHideCompletedChange(true))
        advance()
        assertTrue(viewModel.content().tasks.none { it.isCompleted })
        assertEquals(4, viewModel.content().tasks.size)
    }

    @Test
    fun openTasksFirstSortsCompletedToTheEnd() = transformTest {
        val viewModel = createViewModel()
        advance()

        viewModel.onAction(TaskListAction.OnOpenTasksFirstChange(true))
        advance()
        val tasks = viewModel.content().tasks
        assertEquals(6, tasks.size)
        assertEquals(tasks.sortedBy { it.isCompleted }, tasks)
        assertTrue(tasks.last().isCompleted)
    }

    @Test
    fun resetFiltersClearsAllInputsAtOnce() = transformTest {
        val viewModel = createViewModel()
        advance()

        viewModel.onAction(TaskListAction.OnHideCompletedChange(true))
        viewModel.onAction(TaskListAction.OnOpenTasksFirstChange(true))
        viewModel.searchQuery.setTextAndPlaceCursorAtEnd("x")
        advance()

        viewModel.onAction(TaskListAction.OnResetFiltersClick)
        advance()
        val state = viewModel.content()
        assertEquals("", viewModel.searchQuery.text.toString())
        assertFalse(state.hideCompleted)
        assertFalse(state.openTasksFirst)
        assertEquals(6, state.tasks.size)
    }

    @Test
    fun networkChangesFlowIntoUiState() = transformTest {
        val networkMonitor = FakeNetworkMonitor()
        val viewModel = createViewModel(networkMonitor)
        advance()
        assertTrue(viewModel.content().isOnline)

        networkMonitor.online.value = false
        advance()
        assertFalse(viewModel.content().isOnline)
    }

    @Test
    fun focusSessionEndsAutomatically() = transformTest {
        val viewModel = createViewModel()
        advance()

        val task = viewModel.content().tasks.first()
        viewModel.onAction(TaskListAction.OnFocusClick(task))
        advance()
        assertEquals(task.id, (viewModel.content().focusSession as FocusSession.Running).taskId)

        advanceTimeBy(1.minutes)
        advance()
        advance()
        assertEquals(FocusSession.Idle, viewModel.content().focusSession)
    }
}
