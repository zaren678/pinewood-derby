package com.johnanderson.pinewoodderbyiot.bluetooth

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.content.Context
import java.util.*
import android.os.ParcelUuid
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.BroadcastReceiver
import android.content.Intent
import android.util.Log
import android.content.IntentFilter
import java.io.Closeable
import com.johnanderson.pinewoodderbybleshared.PinewoodDerbyBleConstants
import com.johnanderson.pinewoodderbybleshared.models.MotorState

class BleServer(context: Context) : Closeable {

    private val TAG: String? = BleServer::class.simpleName

    private var mBluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private var mBluetoothAdapter: BluetoothAdapter
    private var mBluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var mBluetoothGattServer: BluetoothGattServer? = null

    private var mContext: Context = context

    private var mRunAfterEnabledQueue: Queue<Runnable> = ArrayDeque<Runnable>()

    private val mBluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val state = intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)

            when (state) {
                BluetoothAdapter.STATE_ON -> {
                    mRunAfterEnabledQueue.forEach { it.run() }
                }
                BluetoothAdapter.STATE_OFF -> {
                    mRunAfterEnabledQueue.clear()
                    stopServer()
                    stopAdvertising()
                }
            }
        }
    }

    private val mAdvertiseCallback = object: AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.e(TAG, "Start success: $settingsInEffect")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "Start Failure: $errorCode")
        }
    }

    private val mGattServerCallback = object : BluetoothGattServerCallback() {
        override fun onCharacteristicWriteRequest(device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            Log.e(TAG, "onCharacteristicWriteRequest")
            if (responseNeeded) {
                mBluetoothGattServer?.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null)
            }
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
            Log.e(TAG, "onCharacteristicReadRequest")
            when (characteristic?.uuid) {
                PinewoodDerbyBleConstants.MOTOR_STATE_CHARACTERISTIC -> {
                    val testMotorState = MotorState.Builder().direction(MotorState.Direction.FORWARD).speed(100).test(true).build()
                    mBluetoothGattServer?.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            MotorState.ADAPTER.encode(testMotorState))
                }
                else -> {
                    Log.w(TAG, "Invalid Characteristic Read: " + characteristic?.getUuid())
                    mBluetoothGattServer?.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null)
                }
            }
        }

        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            Log.e(TAG, "onConnectionStateChange: $device, $status, $newState")
        }
    }

    init {
        mBluetoothAdapter = mBluetoothManager.adapter
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        mContext.registerReceiver(mBluetoothReceiver, filter)
    }

    override fun close() {
        stopServer()
        stopAdvertising()
        mContext.unregisterReceiver(mBluetoothReceiver)
    }

    fun startServer() {
        runAfterEnabled(Runnable {
            startServerImpl()
        })
    }

    fun startAdvertising() {
        runAfterEnabled( Runnable {
            startAdvertisingImpl()
        } )
    }

    fun stopServer() {
        mBluetoothGattServer?.close()
        mBluetoothGattServer = null
    }

    fun stopAdvertising() {
        mBluetoothLeAdvertiser?.stopAdvertising(mAdvertiseCallback)
        mBluetoothLeAdvertiser = null
    }

    private fun runAfterEnabled(r: Runnable) {
        if (mBluetoothAdapter.isEnabled) {
            r.run()
        } else {
            mRunAfterEnabledQueue.add(r)
            mBluetoothAdapter.enable()
        }
    }

    private fun startServerImpl() {
        mBluetoothGattServer = mBluetoothManager.openGattServer(mContext, mGattServerCallback)
        if (mBluetoothGattServer == null) {
            Log.w(TAG, "Unable to create GATT server")
            return
        }

        mBluetoothGattServer?.addService(createService())
    }

    private fun startAdvertisingImpl() {
        val bluetoothAdapter = mBluetoothManager.adapter
        mBluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        if (mBluetoothLeAdvertiser == null) {
            Log.e(TAG, "Failed to create advertiser")
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
                BluetoothGattCharacteristic.PERMISSION_READ)

        service.addCharacteristic(motorState)

        return service
    }
}