package com.example.neuro

import android.app.Application
import android.content.Context

class NeuroApplication : Application() {

    companion object {
        lateinit var instance: NeuroApplication
            private set

        fun getContext(): Context = instance.applicationContext
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
