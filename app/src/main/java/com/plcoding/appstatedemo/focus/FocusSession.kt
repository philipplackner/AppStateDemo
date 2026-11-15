package com.plcoding.appstatedemo.focus

import kotlinx.serialization.Serializable

@Serializable
sealed interface FocusSession {

    @Serializable
    data object Idle : FocusSession

    @Serializable
    data class Running(
        val taskId: String,
        val taskTitle: String,
        val endTimeMillis: Long,
    ) : FocusSession
}
