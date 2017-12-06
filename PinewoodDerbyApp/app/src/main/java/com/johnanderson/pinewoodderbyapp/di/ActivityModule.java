package com.johnanderson.pinewoodderbyapp.di;

import com.johnanderson.pinewoodderbyapp.connect.ConnectActivity;
import com.johnanderson.pinewoodderbyapp.scan.ScanActivity;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class ActivityModule {
    @ContributesAndroidInjector
    abstract ScanActivity contributeScanActivity();

    @ContributesAndroidInjector
    abstract ConnectActivity contributeConnectActivity();
}
