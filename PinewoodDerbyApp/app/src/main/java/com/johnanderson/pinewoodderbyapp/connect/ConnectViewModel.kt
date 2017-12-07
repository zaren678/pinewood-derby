package com.johnanderson.pinewoodderbyapp.connect

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.bluetooth.BluetoothManager
import android.databinding.Bindable
import android.util.Log
import com.johnanderson.pinewoodderbyapp.BR
import com.johnanderson.pinewoodderbyapp.binding.ObservableAndroidViewModel
import com.johnanderson.pinewoodderbyapp.binding.bindable
import com.johnanderson.pinewoodderbyapp.ble.BleClient
import com.johnanderson.pinewoodderbyapp.ble.MotorBleGattServiceClient
import com.johnanderson.pinewoodderbyapp.guava.onComplete
import com.johnanderson.pinewoodderbybleshared.PinewoodDerbyBleConstants
import com.johnanderson.pinewoodderbybleshared.models.MotorState
import javax.inject.Inject

class ConnectViewModel @Inject constructor(mApplication: Application, mBluetoothManager: BluetoothManager): ObservableAndroidViewModel(mApplication) {
    private val TAG: String? = ConnectViewModel::class.java.simpleName

    private var mBleClient: BleClient? = null
    private var mAddress: String? = null
    private var mMotorBleGattServiceClient: MotorBleGattServiceClient? = null

    @get:Bindable
    var example: Int by bindable(0, BR.example)
        private set

    fun setBleAddress(address: String) {
        mAddress = address
    }

    fun setBleClient(bleClient: BleClient) {
        mBleClient = bleClient
    }

    fun connect() {
        if (mBleClient == null || mAddress == null) {
            return
        }
        val future = mBleClient?.connect(mAddress!!)
        future?.onComplete(
                onFailure = { throwable ->
                    //TODO error
                    Log.e(TAG, "Failed to connect: $throwable")
                },
                onSuccess = { _ ->
                    val supportedGattServices = mBleClient?.getSupportedGattServices()
                    val service = supportedGattServices?.find { it.uuid == PinewoodDerbyBleConstants.SERVICE_ID }
                    if (service != null) {
                        mMotorBleGattServiceClient = MotorBleGattServiceClient(mBleClient!!, service)
                    } else {
                        //TODO error
                    }
                })
    }

    fun writeMotorState() {
        if (mBleClient == null || mAddress == null) {
            return
        }

        val motorState = MotorState.Builder().test(true).speed(55).direction(MotorState.Direction.BACKWARD).build()
        Log.e(TAG, "About to write motor state: " + motorState)
        val future = mMotorBleGattServiceClient?.writeMotorState(motorState)
        future?.onComplete(
                onFailure = { throwable ->
                    //TODO error
                    Log.e(TAG, "Failed to write: $throwable")
                },
                onSuccess = { state ->
                    Log.e(TAG, "Motor state written: " + state)
                }
        )
    }

    override fun onCleared() {
        super.onCleared()
        mBleClient?.disconnect()
    }
}