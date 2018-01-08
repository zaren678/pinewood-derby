package com.johnanderson.pinewoodderbyiot.repo

import com.johnanderson.pinewoodderbybleshared.models.MotorState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MotorStateRepo @Inject constructor() {

    private val mListeners:MutableList<()->Unit> = ArrayList()
    private var mMotorState:MotorState = MotorState(0.0, false)

    fun getMotorState(): MotorState {
        synchronized(mMotorState) {
            return mMotorState
        }
    }

    fun updateMotorState(state:MotorState) {
        var changed = false
        synchronized(mMotorState) {
            if (state != mMotorState) {
                mMotorState = state
                changed = true
            }
        }

        if (changed) {
            notifyChangeListeners()
        }
    }

    fun addChangeListener(f:()->Unit) {
        synchronized(mListeners) {
            mListeners.add(f)
        }
    }

    fun removeChangeListener(f:()->Unit) {
        synchronized(mListeners) {
            mListeners.remove(f)
        }
    }

    private fun notifyChangeListeners() {
        synchronized(mListeners) {
            mListeners.forEach { it() }
        }
    }
}