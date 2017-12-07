package com.johnanderson.pinewoodderbyapp.binding

import android.app.Application
import android.arch.lifecycle.AndroidViewModel

open class ObservableAndroidViewModel(application: Application, notifiableObservable: NotifiableObservable = BaseNotifiableObservable())
    : AndroidViewModel(application), NotifiableObservable by notifiableObservable {

    init {
        notifiableObservable.setDelegator(this)
    }
}