package com.johnanderson.pinewoodderbyapp.connect

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.bluetooth.le.ScanResult
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.johnanderson.pinewoodderbyapp.R
import com.johnanderson.pinewoodderbyapp.databinding.FragmentConnectBinding
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class ConnectFragment : DaggerFragment() {

    companion object Factory {
        private val SCAN_RESULT_EXTRA: String = "SCAN_RESULT_EXTRA"
        fun createFragment(scanResult: ScanResult): ConnectFragment {
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
    private var mScanResult: ScanResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProviders.of(this, viewModelFactory).get(ConnectViewModel::class.java)
        mScanResult = arguments?.getParcelable(SCAN_RESULT_EXTRA)
        if (mScanResult != null) {
            mViewModel.setBleAddress(mScanResult?.device?.address!!)
        } else {
            throw IllegalArgumentException("Scan results not in arguments")
        }
        if (savedInstanceState == null) {
            //mViewModel.init()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val binding: FragmentConnectBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_connect, container, false)

        binding.viewModel = mViewModel
        return binding.root
    }
}
