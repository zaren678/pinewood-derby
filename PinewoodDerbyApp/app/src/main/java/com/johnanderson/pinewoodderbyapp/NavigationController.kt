package com.johnanderson.pinewoodderbyapp

import android.bluetooth.le.ScanResult
import android.support.v4.app.FragmentManager
import com.johnanderson.pinewoodderbyapp.connect.ConnectFragment
import com.johnanderson.pinewoodderbyapp.scan.ScanFragment
import javax.inject.Inject

class NavigationController @Inject constructor(private val mainActivity: MainActivity) {
    private val containerId:Int = R.id.container
    private val fragmentManager: FragmentManager = mainActivity.supportFragmentManager

    fun navigateToScan() {
        val scanFragment = ScanFragment()
        fragmentManager.beginTransaction()
                .replace(containerId, scanFragment)
                .commitAllowingStateLoss()
    }

    fun navigateToConnect(scanResult: ScanResult) {
        val scanFragment = ConnectFragment.createFragment(scanResult)
        fragmentManager.beginTransaction()
                .replace(containerId, scanFragment)
                .addToBackStack(null)
                .commitAllowingStateLoss()
    }

}