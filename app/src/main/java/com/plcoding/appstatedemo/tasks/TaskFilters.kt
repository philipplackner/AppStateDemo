package com.plcoding.appstatedemo.tasks

import com.plcoding.appstatedemo.data.Task

internal fun List<Task>.applyFilters(hideCompleted: Boolean, openTasksFirst: Boolean): List<Task> {
    return filter { !hideCompleted || !it.isCompleted }
        .let { if (openTasksFirst) it.sortedBy(Task::isCompleted) else it }
}
