package com.johnanderson.pinewoodderbyapp.scan

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.databinding.Bindable
import android.os.ParcelUuid
import android.util.Log
import com.johnanderson.pinewoodderbyapp.BR
import com.johnanderson.pinewoodderbyapp.binding.ObservableAndroidViewModel
import com.johnanderson.pinewoodderbyapp.binding.bindable
import com.johnanderson.pinewoodderbybleshared.PinewoodDerbyBleConstants
import java.util.*
import javax.inject.Inject
import kotlin.concurrent.timer

class ScanViewModel @Inject constructor(mApplication: Application, mBluetoothManager: BluetoothManager): ObservableAndroidViewModel(mApplication) {
    private val TAG: String? = ScanViewModel::class.java.simpleName

    private val TIMEOUT: Long = 15000
    private val SCAN_BATCH_PERIOD: Long = 1000;

    private val mBluetoothAdapter: BluetoothAdapter = mBluetoothManager.adapter
    private val mBluetoothLeScanner: BluetoothLeScanner = mBluetoothAdapter.bluetoothLeScanner
    private val mScanFilter: ScanFilter = ScanFilter.Builder().setServiceUuid(ParcelUuid(PinewoodDerbyBleConstants.SERVICE_ID)).build()

    private var mTimeoutTimer: Timer? = null

    enum class State {
        SCANNING, ERROR, IDLE
    }

    //Properties
    @get:Bindable
    var state: State by bindable(State.IDLE, BR.state)
        private set

    @get:Bindable
    var deviceFound: ScanResult? by bindable(null, BR.deviceFound)
        private set

    @get:Bindable
    var stateText: String by bindable("Scan for Devices", BR.stateText)
        private set

    @get:Bindable
    var scanText: String by bindable("Start Scan", BR.scanText)
        private set

    private val mScanCallback = object : ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            mTimeoutTimer?.cancel()
            Log.e(TAG, "Scan failed $errorCode")
            state = State.ERROR
            stateText = "Scan failed $errorCode"
            scanText = "Retry Scan"
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            mTimeoutTimer?.cancel()
            val sortedWith = results?.sortedWith(compareByDescending({ it.rssi }))
            Log.e(TAG, "Batch Scan result $sortedWith")
            if (sortedWith?.isEmpty() == false) {
                mBluetoothLeScanner.stopScan(this)
                state = State.IDLE
                stateText = "Scan for Devices"
                scanText = "Start Scan"
                deviceFound = sortedWith[0]
            }
        }
    }

    fun startScanning() {
        val settings = ScanSettings.Builder().setReportDelay(SCAN_BATCH_PERIOD).build()
        mBluetoothLeScanner.startScan(listOf(mScanFilter), settings, mScanCallback)
        state = State.SCANNING
        stateText = "Scanning..."
        mTimeoutTimer = timer("scanTimeout", true, TIMEOUT, TIMEOUT, { scanTimeout() })
    }

    private fun scanTimeout() {
        mBluetoothLeScanner.stopScan(mScanCallback)
        state = State.ERROR
        stateText = "No Devices found"
        scanText = "Retry Scan"
        mTimeoutTimer?.cancel()
    }

    override fun onCleared() {
        Log.e(TAG, "Stop Scan")
        mBluetoothLeScanner.stopScan(mScanCallback)
        state = State.IDLE
    }
}
