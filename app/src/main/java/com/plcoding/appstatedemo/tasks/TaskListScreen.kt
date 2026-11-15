package com.plcoding.appstatedemo.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plcoding.appstatedemo.data.Task
import com.plcoding.appstatedemo.focus.FocusSession
import com.plcoding.appstatedemo.ui.theme.AppStateDemoTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun TaskListRoot(viewModel: TaskListViewModel) {
    val state by viewModel.state

    TaskListScreen(
        state = state,
        searchQuery = viewModel.searchQuery,
        onAction = viewModel::onAction,
    )
}

@Composable
fun TaskListCombineRoot(
    viewModel: TaskListCombineViewModel = viewModel(factory = TaskListCombineViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TaskListScreen(
        state = state,
        searchQuery = viewModel.searchQuery,
        onAction = viewModel::onAction,
    )
}

@Composable
fun TaskListUpdateRoot(
    viewModel: TaskListUpdateViewModel = viewModel(factory = TaskListUpdateViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TaskListScreen(
        state = state,
        searchQuery = viewModel.searchQuery,
        onAction = viewModel::onAction,
    )
}

@Composable
fun TaskListScreen(
    state: TaskListState,
    searchQuery: TextFieldState,
    onAction: (TaskListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Header(isOnline = state.isOnline)

            if (state.focusSession is FocusSession.Running) {
                FocusBanner(
                    session = state.focusSession,
                    onExtend = { onAction(TaskListAction.OnExtendFocusClick) },
                    onStop = { onAction(TaskListAction.OnStopFocusClick) },
                )
            }

            OutlinedTextField(
                state = searchQuery,
                label = { Text("Search tasks") },
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.fillMaxWidth(),
            )

            FilterSwitch(
                text = "Hide completed",
                checked = state.hideCompleted,
                onCheckedChange = { onAction(TaskListAction.OnHideCompletedChange(it)) },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterSwitch(
                    text = "Open tasks first",
                    checked = state.openTasksFirst,
                    onCheckedChange = { onAction(TaskListAction.OnOpenTasksFirstChange(it)) },
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onAction(TaskListAction.OnResetFiltersClick) }) {
                    Text("Reset filters")
                }
            }

            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            } else {
                val focusedTaskId = (state.focusSession as? FocusSession.Running)?.taskId
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.tasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            isFocused = task.id == focusedTaskId,
                            onToggleCompleted = { onAction(TaskListAction.OnTaskCheckedChange(task)) },
                            onStartFocus = { onAction(TaskListAction.OnFocusClick(task)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSwitch(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text)
    }
}

@Composable
private fun Header(isOnline: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Tasks",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (isOnline) "Online" else "Offline",
            style = MaterialTheme.typography.labelLarge,
            color = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun FocusBanner(
    session: FocusSession.Running,
    onExtend: () -> Unit,
    onStop: () -> Unit,
) {
    val secondsLeft by produceState(
        initialValue = secondsUntil(session.endTimeMillis),
        session.endTimeMillis,
    ) {
        while (value > 0) {
            delay(1.seconds)
            value = secondsUntil(session.endTimeMillis)
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Focusing on", style = MaterialTheme.typography.labelMedium)
                Text(text = session.taskTitle, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "%d:%02d left".format(secondsLeft / 60, secondsLeft % 60),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            TextButton(onClick = onExtend) {
                Text("+1 min")
            }
            TextButton(onClick = onStop) {
                Text("Stop")
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: Task,
    isFocused: Boolean,
    onToggleCompleted: () -> Unit,
    onStartFocus: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = task.isCompleted, onCheckedChange = { onToggleCompleted() })
        Text(
            text = task.title,
            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
        if (!task.isCompleted && !isFocused) {
            TextButton(onClick = onStartFocus) {
                Text("Focus")
            }
        }
    }
}

private fun secondsUntil(endTimeMillis: Long): Long {
    return ((endTimeMillis - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
}

@Preview(showBackground = true)
@Composable
private fun TaskListScreenPreview() {
    AppStateDemoTheme {
        TaskListScreen(
            state = TaskListState(
                isLoading = false,
                tasks = listOf(
                    Task(id = "1", title = "Write AppState blog post", isCompleted = false),
                    Task(id = "2", title = "Review pull request #42", isCompleted = true),
                ),
                focusSession = FocusSession.Running(
                    taskId = "1",
                    taskTitle = "Write AppState blog post",
                    endTimeMillis = System.currentTimeMillis() + 45_000,
                ),
            ),
            searchQuery = TextFieldState(),
            onAction = {},
        )
    }
}
