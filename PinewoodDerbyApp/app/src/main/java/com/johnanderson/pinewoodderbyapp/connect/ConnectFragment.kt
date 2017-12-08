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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.johnanderson.pinewoodderbyapp.R
import com.johnanderson.pinewoodderbyapp.ble.BleClientService
import com.johnanderson.pinewoodderbyapp.databinding.FragmentConnectBinding
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class ConnectFragment : DaggerFragment() {

    private val TAG: String = ConnectFragment::class.java.simpleName

    companion object Factory {
        private val SCAN_RESULT_EXTRA: String = "SCAN_RESULT_EXTRA"
        fun createFragment(context: Context, scanResult: ScanResult): ConnectFragment {
            val connectFragment = ConnectFragment()
            val bundle = Bundle()
            bundle.putParcelable(SCAN_RESULT_EXTRA, scanResult);
            connectFragment.arguments = bundle
            return connectFragment
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var mViewModel: ConnectViewModel
    private lateinit var mScanResult: ScanResult

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
        mScanResult = arguments.getParcelable(SCAN_RESULT_EXTRA)
        mViewModel.setBleAddress(mScanResult.device.address)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val binding: FragmentConnectBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_connect, container, false)

        binding.viewModel = mViewModel
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val serviceIntent = Intent(context, BleClientService::class.java)
        activity.bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        activity.unbindService(mServiceConnection)
    }
}
