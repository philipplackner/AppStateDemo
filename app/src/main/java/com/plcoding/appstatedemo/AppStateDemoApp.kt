package com.plcoding.appstatedemo

import android.app.Application

class AppStateDemoApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
