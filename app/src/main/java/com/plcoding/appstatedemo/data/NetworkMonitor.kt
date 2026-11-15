package com.plcoding.appstatedemo.data

import androidx.compose.runtime.State

interface NetworkMonitor {
    val isOnline: State<Boolean>
}
