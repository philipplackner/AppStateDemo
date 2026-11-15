package com.plcoding.appstatedemo.tasks

import com.plcoding.appstatedemo.data.Task

sealed interface TaskListAction {
    data class OnHideCompletedChange(val hide: Boolean) : TaskListAction
    data class OnOpenTasksFirstChange(val enabled: Boolean) : TaskListAction
    data object OnResetFiltersClick : TaskListAction
    data class OnTaskCheckedChange(val task: Task) : TaskListAction
    data class OnFocusClick(val task: Task) : TaskListAction
    data object OnExtendFocusClick : TaskListAction
    data object OnStopFocusClick : TaskListAction
}
