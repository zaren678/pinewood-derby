package com.johnanderson.pinewoodderbyapp.di;

import com.johnanderson.pinewoodderbyapp.connect.ConnectFragment;
import com.johnanderson.pinewoodderbyapp.scan.ScanFragment;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class FragmentBuilderModule {
    @ContributesAndroidInjector
    abstract ScanFragment contributeScanFragment();

    @ContributesAndroidInjector
    abstract ConnectFragment contributeConnectFragment();
}
