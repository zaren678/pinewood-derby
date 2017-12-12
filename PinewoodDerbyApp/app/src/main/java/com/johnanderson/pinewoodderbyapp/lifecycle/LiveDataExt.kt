package com.johnanderson.pinewoodderbyapp.lifecycle

import android.arch.core.util.Function
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations

fun <X,Y> LiveData<X>.switchMap(f: (X) -> LiveData<Y>): LiveData<Y> {
    return Transformations.switchMap(this, Function(f))
}

fun <X,Y> LiveData<X>.map(f: (X) -> Y): LiveData<Y> {
    return Transformations.map(this, Function(f))
}
