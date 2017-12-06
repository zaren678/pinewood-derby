package com.johnanderson.pinewoodderbyapp.connect

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.bluetooth.le.ScanResult
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.johnanderson.pinewoodderbyapp.R
import com.johnanderson.pinewoodderbyapp.ble.BleClientService
import com.johnanderson.pinewoodderbyapp.databinding.ActivityConnectBinding
import com.johnanderson.pinewoodderbyapp.scan.ScanViewModel
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject

class ConnectActivity : DaggerAppCompatActivity() {

    private val TAG: String = ConnectActivity::class.java.simpleName

    companion object IntentFactory {
        private val SCAN_RESULT_EXTRA: String = "SCAN_RESULT_EXTRA"
        fun createIntent(context: Context, scanResult: ScanResult): Intent {
            val intent = Intent(context, ConnectActivity::class.java)
            intent.putExtra(SCAN_RESULT_EXTRA, scanResult)
            return intent
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var mViewModel: ConnectViewModel

    private val mServiceConnection = object: ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val localBinder = service as BleClientService.LocalBinder
            val bleClientService = localBinder.getService()
            bleClientService.initialize()
            Log.e(TAG, "onServiceConnected")
            mViewModel.setBleClient(bleClientService)
            mViewModel.connect()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProviders.of(this, viewModelFactory).get(ConnectViewModel::class.java)

        val binding: ActivityConnectBinding = DataBindingUtil.setContentView(this, R.layout.activity_connect)
        val scanResult: ScanResult = intent.extras.getParcelable(SCAN_RESULT_EXTRA)

        mViewModel.setBleAddress(scanResult.device.address)


        Log.e("TAG", "Scan Result $scanResult")
    }

    override fun onStart() {
        super.onStart()
        val serviceIntent = Intent(this, BleClientService::class.java)
        bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(mServiceConnection)
    }
}
