package com.johnanderson.pinewoodderbyiot

import android.app.Application
import timber.log.Timber.DebugTree
import timber.log.Timber



/**
 * Created by johnanderson on 12/10/17.
 */
class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(DebugTree())
    }
}