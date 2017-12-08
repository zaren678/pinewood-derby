package com.johnanderson.pinewoodderbyapp.scan

import android.Manifest
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.databinding.Observable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.johnanderson.pinewoodderbyapp.NavigationController
import com.johnanderson.pinewoodderbyapp.R
import com.johnanderson.pinewoodderbyapp.databinding.FragmentScanBinding
import dagger.android.support.DaggerFragment
import pub.devrel.easypermissions.EasyPermissions
import javax.inject.Inject

class ScanFragment : DaggerFragment(), EasyPermissions.PermissionCallbacks {

    private val TAG: String = ScanFragment::class.java.simpleName
    private val RC_LOCATION = 123

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject lateinit var navigationController: NavigationController

    private lateinit var mViewModel: ScanViewModel

    private val mDeviceFoundListener = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            val scanResult = mViewModel.deviceFound.get()
            if (scanResult != null) {
                Log.e(TAG, "device found!")
                navigationController.navigateToConnect(scanResult)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProviders.of(this, viewModelFactory).get(ScanViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val binding: FragmentScanBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_scan, container, false)
        binding.viewModel = mViewModel

        mViewModel.deviceFound.addOnPropertyChangedCallback(mDeviceFoundListener)
        EasyPermissions.requestPermissions(this,"Please?", RC_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        mViewModel.deviceFound.removeOnPropertyChangedCallback(mDeviceFoundListener);
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>?) {
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>?) {
        if (requestCode == RC_LOCATION) {
            mViewModel.startScanning();
        }
    }
}
