package com.plcoding.appstatedemo.tasks

import com.plcoding.appstatedemo.data.Task
import com.plcoding.appstatedemo.focus.FocusSession

data class TaskListState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val hideCompleted: Boolean = false,
    val openTasksFirst: Boolean = false,
    val focusSession: FocusSession = FocusSession.Idle,
    val isOnline: Boolean = true,
)
