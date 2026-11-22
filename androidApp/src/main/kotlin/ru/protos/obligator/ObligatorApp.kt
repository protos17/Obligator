package ru.protos.obligator

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import ru.protos.obligator.di.initKoin

class ObligatorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        initKoin {
            androidLogger()
            androidContext(this@ObligatorApp)
        }
    }
}
