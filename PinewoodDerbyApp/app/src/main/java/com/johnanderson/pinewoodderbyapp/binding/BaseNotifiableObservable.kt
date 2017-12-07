package com.johnanderson.pinewoodderbyapp.binding

import android.databinding.Observable
import android.databinding.PropertyChangeRegistry

class BaseNotifiableObservable : NotifiableObservable {

    companion object {
        private const val ALL_PROPERTIES = 0
    }

    private val changeRegistryProperty = lazy { PropertyChangeRegistry() }
    private val changeRegistry: PropertyChangeRegistry by changeRegistryProperty
    private lateinit var delegator: NotifiableObservable

    @Synchronized
    override fun setDelegator(delegator: NotifiableObservable) {
        this.delegator = delegator
    }

    @Synchronized
    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback) {
        changeRegistry.add(callback)
    }

    @Synchronized
    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback) {
        changeRegistry.remove(callback)
    }

    @Synchronized
    override fun notifyChange() {
        if (changeRegistryProperty.isInitialized()) {
            changeRegistry.notifyCallbacks(delegator, ALL_PROPERTIES, null)
        }
    }

    @Synchronized
    override fun notifyPropertyChanged(propertyId: Int) {
        if (changeRegistryProperty.isInitialized()) {
            changeRegistry.notifyCallbacks(delegator, propertyId, null)
        }
    }
}