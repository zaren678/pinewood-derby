package com.johnanderson.pinewoodderbyapp.binding

import android.arch.lifecycle.LifecycleObserver
import android.databinding.Observable

interface NotifiableObservable : Observable, LifecycleObserver {
    fun setDelegator(notifiableObservable: NotifiableObservable)
    fun notifyPropertyChanged(propertyId: Int)
    fun notifyChange()
}
