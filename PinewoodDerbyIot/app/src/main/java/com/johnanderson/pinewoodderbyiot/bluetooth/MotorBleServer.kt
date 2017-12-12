package com.johnanderson.pinewoodderbyiot.bluetooth

import android.app.Application
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import com.johnanderson.pinewoodderbybleshared.BleConstants
import com.johnanderson.pinewoodderbybleshared.PinewoodDerbyBleConstants
import com.johnanderson.pinewoodderbybleshared.models.MotorState
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MotorBleServer @Inject constructor(mApplication: Application, mBluetoothManager: BluetoothManager): BleServer(mApplication, mBluetoothManager) {

    //TODO this should be moved to a repository
    private var mMotorState: MotorState = MotorState.Builder().direction(MotorState.Direction.FORWARD).speed(100).test(true).build()

    private lateinit var mMotorCharacteristic: BluetoothGattCharacteristic
    private val mService: BluetoothGattService

    init {
        mService = initService()
    }

    private fun initService(): BluetoothGattService {
        val service = BluetoothGattService(PinewoodDerbyBleConstants.SERVICE_ID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        mMotorCharacteristic = BluetoothGattCharacteristic(PinewoodDerbyBleConstants.MOTOR_STATE_CHARACTERISTIC,
                BluetoothGattCharacteristic.PROPERTY_READ or
                        BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ or
                        BluetoothGattCharacteristic.PERMISSION_WRITE)

        val motorStateDescriptor = BluetoothGattDescriptor(BleConstants.CLIENT_CONFIG_DESCRIPTOR,
                BluetoothGattCharacteristic.PERMISSION_READ or
                        BluetoothGattCharacteristic.PERMISSION_WRITE)

        val userDescriptor = BluetoothGattDescriptor(BleConstants.USER_DESCRIPTOR,
                BluetoothGattCharacteristic.PERMISSION_READ)
        userDescriptor.value = "Motor State".toByteArray()

        mMotorCharacteristic.addDescriptor(motorStateDescriptor)
        mMotorCharacteristic.addDescriptor(userDescriptor)

        service.addCharacteristic(mMotorCharacteristic)

        return service
    }

    private fun notifyMotorStateChanged() {
        mMotorCharacteristic.value = MotorState.ADAPTER.encode(mMotorState)
        notifyRegisteredDevices(mMotorCharacteristic)
    }

    override fun createService(): BluetoothGattService {
        return mService
    }

    override fun getServiceId(): UUID {
        return PinewoodDerbyBleConstants.SERVICE_ID
    }

    override fun readCharacteristic(char: BluetoothGattCharacteristic): Result {
        val encode = MotorState.ADAPTER.encode(mMotorState)
        return Result(true, encode)
    }

    override fun writeCharacteristic(char: BluetoothGattCharacteristic, data:ByteArray?): Boolean {
        val decode = MotorState.ADAPTER.decode(data)
        if (decode != null) {
            mMotorState = decode
            notifyMotorStateChanged()
            return true
        }

        return false
    }
}