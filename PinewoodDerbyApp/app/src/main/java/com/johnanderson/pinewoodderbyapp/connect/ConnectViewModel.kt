package com.johnanderson.pinewoodderbyapp.connect

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.os.Bundle
import android.util.Log
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import com.johnanderson.pinewoodderbyapp.ble.BleClient
import com.johnanderson.pinewoodderbyapp.ble.BleResultsConstants
import com.johnanderson.pinewoodderbybleshared.PinewoodDerbyBleConstants
import com.johnanderson.pinewoodderbybleshared.models.MotorState
import javax.inject.Inject

class ConnectViewModel @Inject constructor(mApplication: Application, mBluetoothManager: BluetoothManager): AndroidViewModel(mApplication) {
    private val TAG: String? = ConnectViewModel::class.java.simpleName

    private var mBleClient: BleClient? = null
    private var mAddress: String? = null

    fun setBleAddress(address: String) {
        mAddress = address;
    }

    fun setBleClient(bleClient: BleClient) {
        mBleClient = bleClient
    }

    fun connect() {
        if (mBleClient == null || mAddress == null) {
            return
        }
        val future = mBleClient?.connect(mAddress!!)
        if (future != null) {
            Futures.addCallback(future, object : FutureCallback<Bundle> {
                override fun onSuccess(result: Bundle?) {
                    Log.e(TAG, "Successful connect");

                    var supportedGattServices = mBleClient?.getSupportedGattServices()
                    var foundChar: BluetoothGattCharacteristic? = null
                    if (supportedGattServices != null) {
                        for (service in supportedGattServices) {
                            for(characteristic in service.characteristics) {
                                if (characteristic.uuid == PinewoodDerbyBleConstants.MOTOR_STATE_CHARACTERISTIC) {
                                    foundChar = characteristic
                                    break;
                                }
                            }
                        }
                    }

                    if (foundChar != null) {
                        val future1 = mBleClient?.readCharacteristic(foundChar)
                        Futures.addCallback(future1, object : FutureCallback<Bundle> {
                            override fun onSuccess(result: Bundle?) {
                                Log.e(TAG, "Success reading characteristic");
                                val results = result?.getByteArray(BleResultsConstants.CHARACTERISTIC_DATA)
                                val motorState = MotorState.ADAPTER.decode(results)
                                Log.e(TAG, "Characteristic: $motorState")
                            }

                            override fun onFailure(t: Throwable?) {
                                Log.e(TAG, "Failed to read: $t")
                            }

                        }, MoreExecutors.directExecutor())
                    }
                }

                override fun onFailure(t: Throwable?) {
                    Log.e(TAG, "Failed to connect: $t")
                }

            }, MoreExecutors.directExecutor())
        }
    }
}