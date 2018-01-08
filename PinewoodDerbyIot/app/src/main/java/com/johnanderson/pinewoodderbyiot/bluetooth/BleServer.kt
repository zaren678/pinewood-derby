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
import com.johnanderson.pinewoodderbybleshared.BleConstants
import timber.log.Timber
import java.util.*
import kotlin.collections.HashSet

abstract class BleServer(
        private val mContext: Context,
        private val mBluetoothManager: BluetoothManager
) : AutoCloseable {

    private val mBluetoothAdapter: BluetoothAdapter = mBluetoothManager.adapter
    private var mBluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var mBluetoothGattServer: BluetoothGattServer? = null

    private val mRegisteredDevices: MutableSet<BluetoothDevice?> = HashSet()

    private var mDisconnectListener: ()->Unit = {}

    //TODO hide android stuff in an interface
    private val mUiHandler: Handler = Handler(Looper.getMainLooper())

    private var mRunAfterEnabledQueue: Queue<()->Unit> = ArrayDeque<()->Unit>()
    private var mRunAfterDisabledQueue: Queue<()->Unit> = ArrayDeque<()->Unit>()

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

            mBluetoothGattServer?.services?.forEach {
                it.characteristics.filterNotNull().forEach {
                    if (characteristic != null &&
                            characteristic.properties.and(BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 &&
                            it.uuid == characteristic.uuid) {
                        val result = writeCharacteristic(characteristic, value)
                        if (result) {
                            if (responseNeeded) {
                                mBluetoothGattServer?.sendResponse(device,
                                        requestId,
                                        BluetoothGatt.GATT_SUCCESS,
                                        0,
                                        value)
                                return
                            }
                        }
                    }
                }
            }

            if (responseNeeded) {
                mBluetoothGattServer?.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null)
            }
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
            Timber.d("onCharacteristicReadRequest")
            mBluetoothGattServer?.services?.forEach {
                it.characteristics.filterNotNull().forEach {
                    if (characteristic != null &&
                        characteristic.properties.and(BluetoothGattCharacteristic.PROPERTY_READ) != 0 &&
                        it.uuid == characteristic.uuid) {
                        val (result, data) = readCharacteristic(characteristic)
                        if (result) {
                            mBluetoothGattServer?.sendResponse(device,
                                    requestId,
                                    BluetoothGatt.GATT_SUCCESS,
                                    0,
                                    data)
                            return
                        }
                    }
                }
            }

            Timber.w("Invalid Characteristic Read: ${characteristic?.uuid}")
            mBluetoothGattServer?.sendResponse(device,
                    requestId,
                    BluetoothGatt.GATT_FAILURE,
                    0,
                    null)
        }

        override fun onDescriptorReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor?) {
            if (BleConstants.CLIENT_CONFIG_DESCRIPTOR == descriptor?.uuid) {
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
            if (BleConstants.CLIENT_CONFIG_DESCRIPTOR == descriptor?.uuid) {
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
                    mDisconnectListener()
                    startAdvertising()
                }
            }
        }
    }

    init {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        mContext.registerReceiver(mBluetoothReceiver, filter)
    }

    fun notifyRegisteredDevices(char:BluetoothGattCharacteristic) {
        if (mRegisteredDevices.isEmpty()) {
            Timber.d("No subscribers registered")
            return
        }

        Timber.d("Sending update to ${mRegisteredDevices.size} subscribers")
        mRegisteredDevices
                .filterNotNull()
                .forEach { mBluetoothGattServer?.notifyCharacteristicChanged(it, char, false) }
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

    fun setDisconnectListener(f:()->Unit) {
        mDisconnectListener = f
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
                .addServiceUuid(ParcelUuid(getServiceId()))
                .build()

        mBluetoothLeAdvertiser?.startAdvertising(settings, data, mAdvertiseCallback)
    }

    internal abstract fun createService(): BluetoothGattService
    internal abstract fun getServiceId(): UUID

    /**
     * Read a characteristic, result has a boolean and byte array. If the boolean is false then the
     * read operation failed
     */
    internal abstract fun readCharacteristic(char:BluetoothGattCharacteristic): Result

    /**
     * Write a characteristic, result has a boolean. If the boolean is false then the
     * write operation failed
     */
    internal abstract fun writeCharacteristic(char:BluetoothGattCharacteristic, data:ByteArray?): Boolean

    data class Result(val result: Boolean, val data: ByteArray? = null) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Result

            if (result != other.result) return false
            if (!Arrays.equals(data, other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result1 = result.hashCode()
            result1 = 31 * result1 + (data?.let { Arrays.hashCode(it) } ?: 0)
            return result1
        }
    }
}