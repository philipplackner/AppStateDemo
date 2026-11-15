package com.plcoding.appstatedemo.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class TaskRepository {

    private val tasks = MutableStateFlow(
        listOf(
            Task(id = "1", title = "Write AppState blog post", isCompleted = false),
            Task(id = "2", title = "Review pull request #42", isCompleted = true),
            Task(id = "3", title = "Record Compose tutorial", isCompleted = false),
            Task(id = "4", title = "Update Gradle dependencies", isCompleted = false),
            Task(id = "5", title = "Answer community questions", isCompleted = true),
            Task(id = "6", title = "Plan next video", isCompleted = false),
        )
    )

    fun searchTasks(query: String): Flow<List<Task>> {
        return tasks.map { allTasks ->
            allTasks.filter { it.title.contains(query.trim(), ignoreCase = true) }
        }
    }

    fun toggleCompleted(taskId: String) {
        tasks.update { allTasks ->
            allTasks.map { task ->
                if (task.id == taskId) task.copy(isCompleted = !task.isCompleted) else task
            }
        }
    }
}
