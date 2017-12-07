package com.johnanderson.pinewoodderbyapp.ble

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.io.Closeable
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothGattService
import android.os.Bundle
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import java.lang.Exception
import java.util.*


class BleClientService: Service(), Closeable, BleClient {

    private val TAG: String? = BleClientService::class.simpleName

    private val STATE_DISCONNECTED = 0
    private val STATE_CONNECTING = 1
    private val STATE_CONNECTED = 2

    private val mBluetoothManager: BluetoothManager? by lazy {
        getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothDeviceAddress: String? = null
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mConnectionState = STATE_DISCONNECTED

    private val mRequestQueue: Queue<Request> = ArrayDeque()
    private var mCurrentRequest: Request? = null

    private val mBinder = LocalBinder()

    inner class LocalBinder : Binder() {

        fun getService() = this@BleClientService
    }

    private val mGattCallback = object : BluetoothGattCallback() {
        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            Log.e(TAG, "onCharacteristicRead")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val bundle = Bundle()
                bundle.putByteArray(BleResultsConstants.CHARACTERISTIC_DATA, characteristic?.value)
                setResult(bundle)
            } else {
                setError(Exception("Failed to read characteristic"))
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            Log.e(TAG, "onCharacteristicWrite: $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val bundle = Bundle()
                bundle.putByteArray(BleResultsConstants.CHARACTERISTIC_DATA, characteristic?.value)
                setResult(bundle)
            } else {
                setError(Exception("Failed to write characteristic"))
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            Log.e(TAG, "onServicesDiscovered")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val supportedGattServices = getSupportedGattServices()
                Log.e(TAG, "Services discovered: " + supportedGattServices?.joinToString())
                setResult()
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status)
                setError(Exception("Failed to discover services"))
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            Log.e(TAG, "onCharacteristicChanged")
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.e(TAG, "onConnectionStateChange")
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    mConnectionState = STATE_CONNECTED
                    Log.i(TAG, "Connected to GATT server.")
                    // Attempts to discover services after successful connection.
                    Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt?.discoverServices())

                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    mConnectionState = STATE_DISCONNECTED
                    Log.i(TAG, "Disconnected from GATT server.")
                }
            }
        }
    }

    private fun setResult() {
        mCurrentRequest?.setResult()
        mCurrentRequest = null
        runQueue()
    }

    private fun setResult(b: Bundle) {
        mCurrentRequest?.setResult(b)
        mCurrentRequest = null
        runQueue()
    }

    private fun setError(t:Throwable) {
        mCurrentRequest?.setError(t)
        mCurrentRequest = null
        runQueue()
    }

    override fun onBind(intent: Intent?): IBinder {
        return mBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        close()
        return super.onUnbind(intent)
    }

    fun initialize(): Boolean {
        if (mBluetoothManager == null) {
            Log.e(TAG, "Failed to initialize Bluetooth manager")
            return false
        }
        mBluetoothAdapter = mBluetoothManager?.adapter
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }

        return true
    }

    override fun close() {
        disconnect()
    }

    private fun connectInternal(address: String?): Boolean {
        if (mConnectionState != STATE_DISCONNECTED) {
            setError(Exception("Already connected to a device"))
            return false
        }

        if (mBluetoothAdapter == null || address == null) {
            setError(Exception("BluetoothAdapter not initialized or unspecified address."))
            return false
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address == mBluetoothDeviceAddress
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.")
            return if (mBluetoothGatt?.connect() == true) {
                mConnectionState = STATE_CONNECTING
                true
            } else {
                setError(Exception("Failed to connect."))
                false
            }
        }

        val device = mBluetoothAdapter?.getRemoteDevice(address)
        if (device == null) {
            setError(Exception("Device not found.  Unable to connect."))
            return false
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback)
        Log.d(TAG, "Trying to create a new connection.")
        mBluetoothGatt?.connect()
        mBluetoothDeviceAddress = address
        mConnectionState = STATE_CONNECTING
        return true
    }

    private fun readCharacteristicInternal(char: BluetoothGattCharacteristic) {
        if (mConnectionState == STATE_DISCONNECTED) {
            setError(Exception("Device not connected"))
            return
        }

        mBluetoothGatt?.readCharacteristic(char)
    }

    private fun writeCharacteristicInternal(char: BluetoothGattCharacteristic, data:ByteArray) {
        if (mConnectionState == STATE_DISCONNECTED) {
            setError(Exception("Device not connected"))
            return
        }

        char.value = data
        val writeHappened = mBluetoothGatt?.writeCharacteristic(char)
        if (writeHappened == false) {
            setError(Exception("Failed to initiate write"))
        }
    }

    override fun getSupportedGattServices(): List<BluetoothGattService>? {
        return mBluetoothGatt?.services
    }

    private fun addToQueueAndRun(request: Request) {
        mRequestQueue.add(request)
        runQueue()
    }

    private fun runQueue() {
        if (mCurrentRequest == null) {
            mCurrentRequest = mRequestQueue.poll()
            mCurrentRequest?.run()
        }
    }

    override fun connect(address: String): ListenableFuture<Bundle> {
        val request = Request { connectInternal(address) }
        addToQueueAndRun(request)
        return request.future
    }

    override fun disconnect() {
        mBluetoothGatt?.disconnect()
        mBluetoothGatt = null
    }

    override fun readCharacteristic(char: BluetoothGattCharacteristic): ListenableFuture<Bundle> {
        val request = Request { readCharacteristicInternal(char) }
        addToQueueAndRun(request)
        return request.future
    }

    override fun writeCharacteristic(char: BluetoothGattCharacteristic, data: ByteArray): ListenableFuture<Bundle> {
        val request = Request { writeCharacteristicInternal(char, data) }
        addToQueueAndRun(request)
        return request.future
    }

    private inner class Request(val predicate: () -> Unit) {
        private val mFuture: SettableFuture<Bundle> = SettableFuture.create()
        fun setResult(result: Bundle) {
            mFuture.set(result)
        }
        fun setResult() {
            mFuture.set(Bundle())
        }
        fun setError(ex: Throwable) {
            mFuture.setException(ex)
        }

        fun run() {
            predicate()
        }

        val future: ListenableFuture<Bundle>
            get() {
                return mFuture
            }
    }
}