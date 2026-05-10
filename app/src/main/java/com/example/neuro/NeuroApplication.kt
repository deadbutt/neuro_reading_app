package com.example.neuro

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NeuroApplication : Application() {
    
    companion object {
        private lateinit var instance: NeuroApplication
        
        fun getContext(): Context {
            return instance.applicationContext
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}