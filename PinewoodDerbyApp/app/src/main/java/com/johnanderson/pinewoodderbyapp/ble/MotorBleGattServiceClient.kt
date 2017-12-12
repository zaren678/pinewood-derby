package com.johnanderson.pinewoodderbyapp.ble

import android.arch.lifecycle.MutableLiveData
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.os.Bundle
import com.google.common.util.concurrent.ListenableFuture
import com.johnanderson.pinewoodderbyapp.guava.DirectExecutor
import com.johnanderson.pinewoodderbyapp.guava.map
import com.johnanderson.pinewoodderbyapp.guava.onSuccess
import com.johnanderson.pinewoodderbybleshared.PinewoodDerbyBleConstants
import com.johnanderson.pinewoodderbybleshared.models.MotorState

class MotorBleGattServiceClient(private val mBleClient: BleClient, mService: BluetoothGattService): AutoCloseable {

    private var mMotorStateCharacteristic: BluetoothGattCharacteristic

    private val characteristicListener : (BluetoothGattCharacteristic) -> Unit = {
        val value = it.value
        updateMotorState(value)
    }

    val motorState:MutableLiveData<MotorState?> = MutableLiveData()

    init {
        if (mService.uuid != PinewoodDerbyBleConstants.SERVICE_ID) {
            throw IllegalArgumentException("Must use Pinewood derby service")
        }

        mMotorStateCharacteristic = mService.getCharacteristic(PinewoodDerbyBleConstants.MOTOR_STATE_CHARACTERISTIC)
        readMotorState()
        mBleClient.addCharacteristicListener(mMotorStateCharacteristic, characteristicListener)
    }

    private fun readMotorState() {
        val future = mBleClient.readCharacteristic(mMotorStateCharacteristic)
        future.onSuccess {
            updateMotorState(it.getByteArray(BleResultsConstants.CHARACTERISTIC_DATA))
        }
    }

    private fun updateMotorState(it: ByteArray?) {
        if (it != null) {
            val motorState = MotorState.ADAPTER.decode(it)
            this.motorState.postValue(motorState)
        }
    }

    fun writeMotorState(state: MotorState): ListenableFuture<MotorState?> {
        val future = mBleClient.writeCharacteristic(mMotorStateCharacteristic, MotorState.ADAPTER.encode(state))
        return future.map(DirectExecutor, { b: Bundle? ->
            val bytes = b?.getByteArray(BleResultsConstants.CHARACTERISTIC_DATA)
            var returnVar: MotorState? = null
            if (bytes != null) {
                returnVar = MotorState.ADAPTER.decode(bytes)
            }
            returnVar
        })
    }

    override fun close() {
        mBleClient.removeCharacteristicListener(mMotorStateCharacteristic, characteristicListener)
    }
}
