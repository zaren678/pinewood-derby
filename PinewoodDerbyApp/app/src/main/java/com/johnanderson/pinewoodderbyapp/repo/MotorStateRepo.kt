package com.johnanderson.pinewoodderbyapp.repo

import android.arch.lifecycle.LiveData
import com.johnanderson.pinewoodderbyapp.ble.MotorBleGattServiceClient
import com.johnanderson.pinewoodderbybleshared.models.MotorState
import javax.inject.Singleton

/**
 * Created by johnanderson on 12/8/17.
 */
@Singleton
class MotorStateRepo {

    private var mCurrentMotorState: MotorState? = null
    private var mMotorBleClient: MotorBleGattServiceClient? = null
    private val mMotorStateLiveData: MotorStateLiveData = MotorStateLiveData()

    val motorState: LiveData<MotorState?>
        get() {
            return mMotorStateLiveData
        }

    private inner class MotorStateLiveData: LiveData<MotorState?>() {
        fun setMotorState(m:MotorState?) {
            postValue(m)
        }
    }

    fun setMotorBleClient(m:MotorBleGattServiceClient) {
        mMotorBleClient = m
        if (mMotorBleClient != null) {
            //TODO register for notifications
        }
    }
}