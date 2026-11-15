package com.plcoding.appstatedemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.plcoding.appstatedemo.tasks.TaskListRoot
import com.plcoding.appstatedemo.tasks.TaskListViewModel
import com.plcoding.appstatedemo.ui.theme.AppStateDemoTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TaskListViewModel by viewModels { TaskListViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // AppState alpha02: a transform first created inside a UI composition never recomposes
        viewModel
        enableEdgeToEdge()
        setContent {
            AppStateDemoTheme {
                TaskListRoot(viewModel = viewModel)
            }
        }
    }
}
