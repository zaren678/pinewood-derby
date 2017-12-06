package com.johnanderson.pinewoodderbyapp.scan

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.databinding.Bindable
import android.databinding.ObservableBoolean
import android.databinding.ObservableField
import android.os.ParcelUuid
import android.util.Log
import com.johnanderson.pinewoodderbybleshared.PinewoodDerbyBleConstants
import javax.inject.Inject

class ScanViewModel @Inject constructor(mApplication: Application, mBluetoothManager: BluetoothManager): AndroidViewModel(mApplication) {
    private val TAG: String? = ScanViewModel::class.java.simpleName

    private val mBluetoothAdapter: BluetoothAdapter = mBluetoothManager.adapter
    private val mBluetoothLeScanner: BluetoothLeScanner = mBluetoothAdapter.bluetoothLeScanner
    private val mScanFilter: ScanFilter = ScanFilter.Builder().setServiceUuid(ParcelUuid(PinewoodDerbyBleConstants.SERVICE_ID)).build()

    //Properties
    val scanning: ObservableBoolean = ObservableBoolean(false)
    val deviceFound: ObservableField<ScanResult> = ObservableField()
    val error: ObservableBoolean = ObservableBoolean(false)

    private val mScanCallback = object : ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "Scan failed $errorCode")
            error.set(true)
            scanning.set(false)
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            Log.e(TAG, "Scan result $callbackType, $result")
            if (result != null) {
                mBluetoothLeScanner.stopScan(this)
                scanning.set(false)
                deviceFound.set(result)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            Log.e(TAG, "Batch Scan result $results")
        }
    }

    fun startScanning() {
        mBluetoothLeScanner.startScan(listOf(mScanFilter), ScanSettings.Builder().build(), mScanCallback);
        error.set(false)
        scanning.set(true)
    }
}
