package com.johnanderson.pinewoodderbyapp.binding

import android.arch.lifecycle.ViewModel

open class ObservableViewModel(notifiableObservable: NotifiableObservable = BaseNotifiableObservable())
    : ViewModel(), NotifiableObservable by notifiableObservable {

    init {
        notifiableObservable.setDelegator(this)
    }
}
