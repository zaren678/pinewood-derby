package com.johnanderson.pinewoodderbyapp.ble

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.johnanderson.pinewoodderbyapp.ext.isNullOrEmpty
import com.johnanderson.pinewoodderbyapp.guava.scheduledExecutorService
import com.johnanderson.pinewoodderbyapp.guava.toListenableFuture
import com.johnanderson.pinewoodderbybleshared.BleConstants
import timber.log.Timber
import java.io.Closeable
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit


class BleClientService: Service(), Closeable, BleClient {

    private val CONNECT_TAG = "Connect"
    private val TIMEOUT_MS: Long = 45000

    private enum class ConnectedState {
        STATE_DISCONNECTED,
        STATE_CONNECTING,
        STATE_CONNECTED
    }

    private val mBluetoothManager: BluetoothManager? by lazy {
        getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothDeviceAddress: String? = null
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mConnectionState = ConnectedState.STATE_DISCONNECTED

    private val mRequestQueue: Queue<Request> = ArrayDeque()
    private var mCurrentRequest: Request? = null

    private val mRegisteredListeners:MutableMap<BluetoothGattCharacteristic, MutableList<(BluetoothGattCharacteristic)->Unit>> = HashMap()

    private val mBinder = LocalBinder()

    inner class LocalBinder : Binder() {

        fun getService() = this@BleClientService
    }

    private val mGattCallback = object : BluetoothGattCallback() {
        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            Timber.d("onCharacteristicRead")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val bundle = Bundle()
                bundle.putByteArray(BleResultsConstants.CHARACTERISTIC_DATA, characteristic?.value)
                setResult(bundle)
            } else {
                setError(Exception("Failed to read characteristic"))
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            Timber.d("onCharacteristicWrite: $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val bundle = Bundle()
                bundle.putByteArray(BleResultsConstants.CHARACTERISTIC_DATA, characteristic?.value)
                setResult(bundle)
            } else {
                setError(Exception("Failed to write characteristic"))
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                setResult()
            } else {
                setError(Exception("Failed to write gatt descriptor"))
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            Timber.d("onServicesDiscovered")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val supportedGattServices = getSupportedGattServices()
                Timber.d("Services discovered: " + supportedGattServices?.joinToString())
                setResult()
            } else {
                Timber.w("onServicesDiscovered received: " + status)
                setError(Exception("Failed to discover services"))
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            Timber.d("onCharacteristicChanged")
            if (characteristic != null) {
                val callbacks = mRegisteredListeners[characteristic]
                callbacks?.forEach { it(characteristic) }
            }

        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Timber.d("onConnectionStateChange")
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    mConnectionState = ConnectedState.STATE_CONNECTED
                    Timber.d("Connected to GATT server.")
                    // Attempts to discover services after successful connection.
                    Timber.d("Attempting to start service discovery:" + mBluetoothGatt?.discoverServices())

                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    mConnectionState = ConnectedState.STATE_DISCONNECTED
                    Timber.d("Disconnected from GATT server.")

                    if (mCurrentRequest?.tag == CONNECT_TAG) {
                        Timber.e("Failed to connect");
                        setError(Exception("Disconnected while connecting"))
                    }
                }
            }
        }
    }

    private fun setResult() {
        Timber.d("Setting result for " + mCurrentRequest?.tag)
        mCurrentRequest?.setResult()
        mCurrentRequest = null
        runQueue()
    }

    private fun setResult(b: Bundle) {
        Timber.d("Setting result bundle for " + mCurrentRequest?.tag)
        mCurrentRequest?.setResult(b)
        mCurrentRequest = null
        runQueue()
    }

    private fun setError(t:Throwable) {
        Timber.d("Setting error for " + mCurrentRequest?.tag + ", " + t)
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
            Timber.e("Failed to initialize Bluetooth manager")
            return false
        }
        mBluetoothAdapter = mBluetoothManager?.adapter
        if (mBluetoothAdapter == null) {
            Timber.e("Unable to obtain a BluetoothAdapter.")
            return false
        }

        return true
    }

    override fun close() {
        disconnect()
    }

    private fun connectInternal(address: String?): Boolean {
        Timber.d("ConnectInternal: $address")
        if (mConnectionState != ConnectedState.STATE_DISCONNECTED) {
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
            Timber.d("Trying to use an existing mBluetoothGatt for connection.")
            return if (mBluetoothGatt?.connect() == true) {
                mConnectionState = ConnectedState.STATE_CONNECTING
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
        Timber.d("Trying to create a new connection.")
        mBluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(this, false, mGattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(this, false, mGattCallback)
        }
	    val connect = mBluetoothGatt?.connect()
        Timber.d("Started connecting: $connect")
        mBluetoothDeviceAddress = address
        mConnectionState = ConnectedState.STATE_CONNECTING
        return true
    }

    private fun readCharacteristicInternal(char: BluetoothGattCharacteristic) {
        if (mConnectionState != ConnectedState.STATE_CONNECTED) {
            setError(Exception("Device not connected"))
            return
        }

        mBluetoothGatt?.readCharacteristic(char)
    }

    private fun writeCharacteristicInternal(char: BluetoothGattCharacteristic, data:ByteArray) {
        Timber.d("writeCharacteristicInternal")
        if (mConnectionState != ConnectedState.STATE_CONNECTED) {
            setError(Exception("Device not connected"))
            return
        }

        char.value = data
        val writeHappened = mBluetoothGatt?.writeCharacteristic(char)
        if (writeHappened == false) {
            setError(Exception("Failed to initiate write"))
        }
    }

    private fun addCharacteristicListenerInternal(char: BluetoothGattCharacteristic) {
        if (mConnectionState != ConnectedState.STATE_CONNECTED) {
            setError(Exception("Device not connected"))
            return
        }

        if( (char.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0 ) {
            setError(Exception("Characteristic is not notifiable"))
        }

        val descriptor = char.getDescriptor(BleConstants.CLIENT_CONFIG_DESCRIPTOR)
        if (descriptor == null) {
            setError(Exception("Characteristic does not have client configuration"))
        }

        mBluetoothGatt?.setCharacteristicNotification(char, true)
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        mBluetoothGatt?.writeDescriptor(descriptor)
    }

    private fun removeCharacteristicListenerInternal(char: BluetoothGattCharacteristic) {
        if (mConnectionState != ConnectedState.STATE_CONNECTED) {
            setError(Exception("Device not connected"))
            return
        }

        if( (char.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0 ) {
            setError(Exception("Characteristic is not notifiable"))
        }

        val descriptor = char.getDescriptor(BleConstants.CLIENT_CONFIG_DESCRIPTOR)
        if (descriptor == null) {
            setError(Exception("Characteristic does not have client configuration"))
        }

        mBluetoothGatt?.setCharacteristicNotification(char, false)
        descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        mBluetoothGatt?.writeDescriptor(descriptor)
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
        val request = Request(CONNECT_TAG, { connectInternal(address) })
        addToQueueAndRun(request)
        return request.future
    }

    override fun disconnect() {
        Timber.d("Disconnect")
        mBluetoothGatt?.disconnect()
        mBluetoothGatt?.close()
        mBluetoothGatt = null
    }

    override fun readCharacteristic(char: BluetoothGattCharacteristic): ListenableFuture<Bundle> {
        val request = Request("ReadCharacteristic", { readCharacteristicInternal(char) })
        addToQueueAndRun(request)
        return request.future
    }

    override fun writeCharacteristic(char: BluetoothGattCharacteristic, data: ByteArray): ListenableFuture<Bundle> {
        val request = Request( "WriteCharacteristic", { writeCharacteristicInternal(char, data) })
        addToQueueAndRun(request)
        return request.future
    }

    override fun addCharacteristicListener(char: BluetoothGattCharacteristic, f: (c: BluetoothGattCharacteristic) -> Unit): ListenableFuture<Bundle> {
        var list = mRegisteredListeners[char]
        if (list == null) {
            list = ArrayList()
            mRegisteredListeners[char] = list
        }

        return if (list.isEmpty()) {
            list.add(f)
            val request = Request("AddCharacteristicListener", { addCharacteristicListenerInternal(char) })
            addToQueueAndRun(request)
            request.future
        } else {
            list.add(f)
            Bundle().toListenableFuture()
        }
    }

    override fun removeCharacteristicListener(char: BluetoothGattCharacteristic, f: (c: BluetoothGattCharacteristic) -> Unit): ListenableFuture<Bundle> {
        val list = mRegisteredListeners[char]
        list?.remove(f)

        return if (list.isNullOrEmpty()) {
            val request = Request("RemoveCharacteristicListener", { removeCharacteristicListenerInternal(char) })
            addToQueueAndRun(request)
            request.future
        } else {
            //still have listeners... just notify the future
            Bundle().toListenableFuture()
        }
    }

    private inner class Request(val tag: String, val predicate: () -> Unit) {
        private val mFuture: SettableFuture<Bundle> = SettableFuture.create()
        private val mReturnFuture = mFuture.withTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS, scheduledExecutorService)

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
                return mReturnFuture
            }
    }
}