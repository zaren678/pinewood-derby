package com.johnanderson.pinewoodderbyiot.bluetooth

import android.app.Application
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import com.johnanderson.pinewoodderbybleshared.BleConstants
import com.johnanderson.pinewoodderbybleshared.PinewoodDerbyBleConstants
import com.johnanderson.pinewoodderbybleshared.models.MotorState
import com.johnanderson.pinewoodderbyiot.repo.MotorStateRepo
import java.util.*
import javax.inject.Inject

class MotorBleServer @Inject constructor(
        mApplication: Application,
        mBluetoothManager: BluetoothManager,
        private val mMotorStateRepo: MotorStateRepo
): BleServer(mApplication, mBluetoothManager), AutoCloseable {

    private lateinit var mMotorCharacteristic: BluetoothGattCharacteristic
    private val mService: BluetoothGattService

    private val mChangeListener:()->Unit = {
        notifyMotorStateChanged()
    }

    init {
        mService = initService()
        mMotorStateRepo.addChangeListener(mChangeListener)
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
        mMotorCharacteristic.value = MotorState.ADAPTER.encode(mMotorStateRepo.getMotorState())
        notifyRegisteredDevices(mMotorCharacteristic)
    }

    override fun createService(): BluetoothGattService {
        return mService
    }

    override fun getServiceId(): UUID {
        return PinewoodDerbyBleConstants.SERVICE_ID
    }

    override fun readCharacteristic(char: BluetoothGattCharacteristic): Result {
        return when {
            char.uuid == PinewoodDerbyBleConstants.MOTOR_STATE_CHARACTERISTIC -> {
                val encode = MotorState.ADAPTER.encode(mMotorStateRepo.getMotorState())
                Result(true, encode)
            }
            else -> Result(false)
        }
    }

    override fun writeCharacteristic(char: BluetoothGattCharacteristic, data:ByteArray?): Boolean {
        when {
            char.uuid == PinewoodDerbyBleConstants.MOTOR_STATE_CHARACTERISTIC -> {
                val decode = MotorState.ADAPTER.decode(data)
                if (decode != null) {
                    mMotorStateRepo.updateMotorState(decode)
                    return true
                }
            }
        }
        return false
    }
}