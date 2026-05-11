package com.example.magnifier

import android.app.Application
import com.example.magnifier.di.AppContainer

class MagnifierApplication : Application() {
    val container: AppContainer by lazy { AppContainer(applicationContext) }
}
