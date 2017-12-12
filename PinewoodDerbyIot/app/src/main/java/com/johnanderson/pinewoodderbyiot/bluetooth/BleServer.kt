package com.johnanderson.pinewoodderbyiot.bluetooth

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import com.johnanderson.pinewoodderbybleshared.PinewoodDerbyBleConstants
import com.johnanderson.pinewoodderbybleshared.models.MotorState
import timber.log.Timber
import java.io.Closeable
import java.util.*
import kotlin.collections.HashSet

class BleServer(context: Context) : Closeable {

    private var mBluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private var mBluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var mBluetoothGattServer: BluetoothGattServer? = null

    private val mRegisteredDevices: MutableSet<BluetoothDevice?> = HashSet()

    private var mContext: Context = context

    //TODO hide android stuff in an interface
    private val mUiHandler: Handler = Handler(Looper.getMainLooper())

    private var mRunAfterEnabledQueue: Queue<()->Unit> = ArrayDeque<()->Unit>()
    private var mRunAfterDisabledQueue: Queue<()->Unit> = ArrayDeque<()->Unit>()

    //TODO this should be moved to a repository
    private lateinit var mMotorState: MotorState

    private val mBluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val state = intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)

            when (state) {

                BluetoothAdapter.STATE_ON -> {
                    Timber.d("Bluetooth ON")
                    mRunAfterEnabledQueue.forEach { it() }
                    mRunAfterEnabledQueue.clear()
                }
                BluetoothAdapter.STATE_OFF -> {
                    Timber.d("Bluetooth OFF")
                    mRunAfterEnabledQueue.clear()
                    stopServer()
                    stopAdvertising()
                    mRunAfterDisabledQueue.forEach { it() }
                    mRunAfterDisabledQueue.clear()
                }
            }
        }
    }

    private val mAdvertiseCallback = object: AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Timber.d("Start success: $settingsInEffect")
        }

        override fun onStartFailure(errorCode: Int) {
            Timber.e("Start Failure: $errorCode")

            if (errorCode == ADVERTISE_FAILED_TOO_MANY_ADVERTISERS) {
                runAfterDisabled {
                    mBluetoothAdapter.enable()
                    runAfterEnabled({
                        startAdvertising()
                        startServer()
                    })
                }
            }
        }
    }

    private val mGattServerCallback = object : BluetoothGattServerCallback() {
        override fun onCharacteristicWriteRequest(device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            Timber.d("onCharacteristicWriteRequest $responseNeeded")

            val decode = MotorState.ADAPTER.decode(value)
            if (decode != null) {
                mMotorState = decode
                notifyRegisteredDevices()
            }

            if (responseNeeded) {
                mBluetoothGattServer?.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        MotorState.ADAPTER.encode(mMotorState))
            }
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
            Timber.d("onCharacteristicReadRequest")
            when (characteristic?.uuid) {
                PinewoodDerbyBleConstants.MOTOR_STATE_CHARACTERISTIC -> {
                    mBluetoothGattServer?.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            MotorState.ADAPTER.encode(mMotorState))
                }
                else -> {
                    Timber.w("Invalid Characteristic Read: ${characteristic?.uuid}")
                    mBluetoothGattServer?.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null)
                }
            }
        }

        override fun onDescriptorReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor?) {
            if (PinewoodDerbyBleConstants.CLIENT_CONFIG_DESCRIPTOR == descriptor?.uuid) {
                Timber.d("Config descriptor read")
                val returnValue: ByteArray = if (mRegisteredDevices.contains(device)) {
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                } else {
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                }
                mBluetoothGattServer?.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        returnValue)
            } else {
                Timber.w("Unknown descriptor read request")
                mBluetoothGattServer?.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null)
            }
        }

        override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
            Timber.d("onNotificationSent: $device")
        }

        override fun onDescriptorWriteRequest(device: BluetoothDevice?, requestId: Int, descriptor: BluetoothGattDescriptor?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            if (PinewoodDerbyBleConstants.CLIENT_CONFIG_DESCRIPTOR == descriptor?.uuid) {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    Timber.d("Subscribe device to notifications: $device")
                    mRegisteredDevices.add(device)
                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                    Timber.d("Unsubscribe device from notifications: $device")
                    mRegisteredDevices.remove(device)
                }

                if (responseNeeded) {
                    mBluetoothGattServer?.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null)
                }
            } else {
                Timber.w("Unknown descriptor write request")
                if (responseNeeded) {
                    mBluetoothGattServer?.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null)
                }
            }
        }

        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            Timber.d("onConnectionStateChange: $device, $status, $newState")

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mUiHandler.post {
                    stopAdvertising()
                }
            } else {
                mUiHandler.post {
                    startAdvertising()
                }
            }
        }
    }

    init {
        mBluetoothAdapter = mBluetoothManager.adapter
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        mContext.registerReceiver(mBluetoothReceiver, filter)

        mMotorState = MotorState.Builder().direction(MotorState.Direction.FORWARD).speed(100).test(true).build()
    }

    private fun notifyRegisteredDevices() {
        if (mRegisteredDevices.isEmpty()) {
            Timber.d("No subscribers registered")
            return
        }

        Timber.d("Sending update to ${mRegisteredDevices.size} subscribers")
        for (device in mRegisteredDevices) {
            val characteristic = mBluetoothGattServer
                    ?.getService(PinewoodDerbyBleConstants.SERVICE_ID)
                    ?.getCharacteristic(PinewoodDerbyBleConstants.MOTOR_STATE_CHARACTERISTIC)
            characteristic?.value = MotorState.ADAPTER.encode(mMotorState)
            if (device != null && characteristic != null) {
                mBluetoothGattServer?.notifyCharacteristicChanged(device, characteristic, false)
            }
        }
    }

    override fun close() {
        Timber.d("close")
        stopServer()
        stopAdvertising()
        mContext.unregisterReceiver(mBluetoothReceiver)
    }

    fun startServer() {
        runAfterEnabled({
            startServerImpl()
        })
    }

    fun startAdvertising() {
        runAfterEnabled({
            startAdvertisingImpl()
        } )
    }

    fun stopServer() {
        Timber.d("Stop server")
        mBluetoothGattServer?.close()
        mBluetoothGattServer = null
    }

    fun stopAdvertising() {
        Timber.d("Stop advertising")
        mBluetoothLeAdvertiser?.stopAdvertising(mAdvertiseCallback)
        mBluetoothLeAdvertiser = null
    }

    private fun runAfterEnabled(f:()->Unit) {
        mUiHandler.post {
            if (mBluetoothAdapter.isEnabled) {
                f()
            } else {
                mRunAfterEnabledQueue.add(f)
                mBluetoothAdapter.enable()
            }
        }
    }

    private fun runAfterDisabled(f:()->Unit) {
        mUiHandler.post {
            if (!mBluetoothAdapter.isEnabled) {
                f()
            } else {
                mRunAfterEnabledQueue.add(f)
                mBluetoothAdapter.disable()
            }
        }
    }

    private fun startServerImpl() {
        Timber.d("Start server")
        mBluetoothGattServer = mBluetoothManager.openGattServer(mContext, mGattServerCallback)
        if (mBluetoothGattServer == null) {
            Timber.w("Unable to create GATT server")
            return
        }

        mBluetoothGattServer?.addService(createService())
    }

    private fun startAdvertisingImpl() {
        Timber.d( "Start advertising")
        val bluetoothAdapter = mBluetoothManager.adapter
        mBluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        if (mBluetoothLeAdvertiser == null) {
            Timber.e("Failed to create advertiser")
            return
        }

        val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build()

        val data = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(ParcelUuid(PinewoodDerbyBleConstants.SERVICE_ID))
                .build()

        mBluetoothLeAdvertiser?.startAdvertising(settings, data, mAdvertiseCallback)
    }

    private fun createService(): BluetoothGattService {
        val service = BluetoothGattService(PinewoodDerbyBleConstants.SERVICE_ID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val motorState = BluetoothGattCharacteristic(PinewoodDerbyBleConstants.MOTOR_STATE_CHARACTERISTIC,
                BluetoothGattCharacteristic.PROPERTY_READ or
                BluetoothGattCharacteristic.PROPERTY_WRITE or
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ or
                        BluetoothGattCharacteristic.PERMISSION_WRITE)

        val motorStateDescriptor = BluetoothGattDescriptor(PinewoodDerbyBleConstants.CLIENT_CONFIG_DESCRIPTOR,
                BluetoothGattCharacteristic.PERMISSION_READ or
                        BluetoothGattCharacteristic.PERMISSION_WRITE)

        val userDescriptor = BluetoothGattDescriptor(PinewoodDerbyBleConstants.USER_DESCRIPTOR,
                BluetoothGattCharacteristic.PERMISSION_READ)
        userDescriptor.value = "Motor State".toByteArray()

        motorState.addDescriptor(motorStateDescriptor)
        motorState.addDescriptor(userDescriptor)

        service.addCharacteristic(motorState)

        return service
    }
}