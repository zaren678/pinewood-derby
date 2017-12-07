package com.johnanderson.pinewoodderbyapp.binding

fun <T> NotifiableObservable.bindable(initialValue: T, propertyId: Int) : BindableProperty<T> {
    return BindableProperty(initialValue = initialValue,
            observable = this,
            propertyId = propertyId)
}