package com.johnanderson.pinewoodderbyapp.ble

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.os.Bundle
import com.google.common.util.concurrent.ListenableFuture
import com.johnanderson.pinewoodderbyapp.guava.DirectExecutor
import com.johnanderson.pinewoodderbyapp.guava.ImmediateFuture
import com.johnanderson.pinewoodderbyapp.guava.map
import com.johnanderson.pinewoodderbybleshared.PinewoodDerbyBleConstants
import com.johnanderson.pinewoodderbybleshared.models.MotorState

class MotorBleGattServiceClient(private val mBleClient: BleClient, mService: BluetoothGattService) {

    private var mMotorStateCharacteristic: BluetoothGattCharacteristic? = null

    init {
        if (mService.uuid != PinewoodDerbyBleConstants.SERVICE_ID) {
            throw IllegalArgumentException("Must use Pinewood derby service")
        }

        mMotorStateCharacteristic = mService.getCharacteristic(PinewoodDerbyBleConstants.MOTOR_STATE_CHARACTERISTIC)
    }

    fun readMotorState(): ListenableFuture<MotorState?> {
        mMotorStateCharacteristic?.let {
            val future = mBleClient.readCharacteristic(it)
            val map = future.map(DirectExecutor, { b: Bundle? ->
                val bytes = b?.getByteArray(BleResultsConstants.CHARACTERISTIC_DATA)
                var returnVar: MotorState? = null
                if (bytes != null) {
                    returnVar = MotorState.ADAPTER.decode(bytes)
                }
                returnVar
            })
            return map
        }
        return ImmediateFuture { throw IllegalArgumentException("Characteristic not found") }
    }

    fun writeMotorState(state: MotorState): ListenableFuture<MotorState?> {
        mMotorStateCharacteristic?.let {
            val future = mBleClient.writeCharacteristic(it, MotorState.ADAPTER.encode(state))
            val map = future.map(DirectExecutor, { b: Bundle? ->
                val bytes = b?.getByteArray(BleResultsConstants.CHARACTERISTIC_DATA)
                var returnVar: MotorState? = null
                if (bytes != null) {
                    returnVar = MotorState.ADAPTER.decode(bytes)
                }
                returnVar
            })
            return map
        }
        return ImmediateFuture { throw IllegalArgumentException("Characteristic not found") }
    }
}
