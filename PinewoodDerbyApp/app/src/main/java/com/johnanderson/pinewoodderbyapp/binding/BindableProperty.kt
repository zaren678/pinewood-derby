package com.johnanderson.pinewoodderbyapp.binding

import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

/**
 * A property delegate for properties of [NotifiableObservable] objects.
 */
class BindableProperty<T>(initialValue: T,
                          private val observable: NotifiableObservable,
                          private val propertyId: Int) : ObservableProperty<T>(initialValue) {

    override fun beforeChange(property: KProperty<*>, oldValue: T, newValue: T) = oldValue != newValue

    override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
        observable.notifyPropertyChanged(propertyId)
    }
}