package com.johnanderson.pinewoodderbyapp.connect

import android.app.Application
import android.arch.lifecycle.Observer
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.databinding.Bindable
import android.os.Handler
import android.os.IBinder
import com.johnanderson.pinewoodderbyapp.BR
import com.johnanderson.pinewoodderbyapp.binding.ObservableAndroidViewModel
import com.johnanderson.pinewoodderbyapp.binding.bindable
import com.johnanderson.pinewoodderbyapp.ble.BleClient
import com.johnanderson.pinewoodderbyapp.ble.BleClientService
import com.johnanderson.pinewoodderbyapp.ble.MotorBleGattServiceClient
import com.johnanderson.pinewoodderbyapp.guava.onComplete
import com.johnanderson.pinewoodderbybleshared.PinewoodDerbyBleConstants
import com.johnanderson.pinewoodderbybleshared.models.MotorState
import timber.log.Timber
import javax.inject.Inject

class ConnectViewModel @Inject constructor(private val mApplication: Application): ObservableAndroidViewModel(mApplication) {

    enum class State {
        CONNECTING, ERROR, CONNECTED
    }

    private var mBleClient: BleClient? = null
    private var mAddress: String? = null
    private var mMotorBleGattServiceClient: MotorBleGattServiceClient? = null

    private val mMotorStateObserver = Observer<MotorState?> { t -> Timber.d("Motor State Changed: $t") }

    @get:Bindable
    var state: State by bindable(State.CONNECTING, BR.state)
        private set

    @get:Bindable
    var errorText: String by bindable("", BR.errorText)
        private set

    private val mServiceConnection = object: ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val localBinder = service as BleClientService.LocalBinder
            val bleClientService = localBinder.getService()
            bleClientService.initialize()
            Timber.d("onServiceConnected")
            setBleClient(bleClientService)
            Handler().postDelayed({
                connect()
            }, 1000)
        }
    }

    init {
        Timber.d("Init")
        val serviceIntent = Intent(mApplication, BleClientService::class.java)
        mApplication.bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
    }

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
        state = State.CONNECTING
        val future = mBleClient?.connect(mAddress!!)
        future?.onComplete(
                onFailure = { throwable ->
                    Timber.e("Failed to connect: $throwable")
                    state = State.ERROR
                    errorText = "Failed to connect: $throwable"
                },
                onSuccess = { _ ->
                    Timber.d("Connected!!")
                    state = State.CONNECTED
                    val supportedGattServices = mBleClient?.getSupportedGattServices()
                    val service = supportedGattServices?.find { it.uuid == PinewoodDerbyBleConstants.SERVICE_ID }
                    if (service != null) {
                        mMotorBleGattServiceClient = MotorBleGattServiceClient(mBleClient!!, service)
                        mMotorBleGattServiceClient?.motorState?.observeForever(mMotorStateObserver)
                    } else {
                        state = State.ERROR
                        errorText = "Motor State Service not found"
                    }
                })
    }

    fun writeMotorState() {
        val motorState = MotorState.Builder().test(true).speed(55).direction(MotorState.Direction.BACKWARD).build()
        writeMotorState(motorState)
    }

    private fun writeMotorState(motorState:MotorState) {
        if (mBleClient == null || mAddress == null) {
            return
        }

        Timber.d("About to write motor state: $motorState")
        val future = mMotorBleGattServiceClient?.writeMotorState(motorState)
        future?.onComplete(
                onFailure = { throwable ->
                    //TODO error
                    Timber.e("Failed to write: $throwable")
                },
                onSuccess = { state ->
                    Timber.d("Motor state written: $state")
                }
        )
    }

    fun setLight(b:Boolean) {
        val motorState = MotorState.Builder().test(b).speed(55).direction(MotorState.Direction.BACKWARD).build()
        writeMotorState(motorState)
    }

    override fun onCleared() {
        Timber.d("onCleared")
        super.onCleared()
        mMotorBleGattServiceClient?.motorState?.removeObserver(mMotorStateObserver)
        mMotorBleGattServiceClient?.close()
        mBleClient?.disconnect()
        mApplication.unbindService(mServiceConnection)
    }
}