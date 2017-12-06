package com.johnanderson.pinewoodderbyapp.scan

import android.Manifest
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.databinding.Observable
import android.os.Bundle
import android.util.Log
import com.johnanderson.pinewoodderbyapp.R
import com.johnanderson.pinewoodderbyapp.connect.ConnectActivity
import com.johnanderson.pinewoodderbyapp.databinding.ActivityMainBinding
import dagger.android.support.DaggerAppCompatActivity
import pub.devrel.easypermissions.EasyPermissions
import javax.inject.Inject

class ScanActivity : DaggerAppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private val TAG: String = ScanActivity::class.java.simpleName
    private val RC_LOCATION = 123

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var mViewModel: ScanViewModel

    private val mDeviceFoundListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            val scanResult = mViewModel.deviceFound.get()
            if (scanResult != null) {
                Log.e(TAG, "device found!")
                val intent = ConnectActivity.createIntent(this@ScanActivity, scanResult)
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProviders.of(this, viewModelFactory).get(ScanViewModel::class.java)

        val binding: ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.viewModel = mViewModel

        mViewModel.deviceFound.addOnPropertyChangedCallback(mDeviceFoundListener)

        EasyPermissions.requestPermissions(this,"Please?", RC_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    override fun onDestroy() {
        super.onDestroy()
        mViewModel.deviceFound.removeOnPropertyChangedCallback(mDeviceFoundListener);
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>?) {
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>?) {
        if (requestCode == RC_LOCATION) {
            mViewModel.startScanning();
        }
    }
}
