package com.plcoding.appstatedemo.basics

import androidx.appstate.transform.transform
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

class GreetingViewModel : ViewModel() {

    var firstName by mutableStateOf("")
        private set

    var lastName by mutableStateOf("")
        private set

    val greeting: State<String> = transform(
        initialValue = "",
        scope = viewModelScope,
    ) {
        if (firstName.isBlank() && lastName.isBlank()) {
            "Hello, stranger!"
        } else {
            "Hello, $firstName $lastName!"
        }
    }

    fun onFirstNameChange(name: String) {
        firstName = name
    }

    fun onLastNameChange(name: String) {
        lastName = name
    }
}
