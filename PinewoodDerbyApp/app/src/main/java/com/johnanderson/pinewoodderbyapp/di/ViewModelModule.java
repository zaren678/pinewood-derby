package com.johnanderson.pinewoodderbyapp.di;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

import com.johnanderson.pinewoodderbyapp.connect.ConnectViewModel;
import com.johnanderson.pinewoodderbyapp.scan.ScanViewModel;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module
public abstract class ViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(ScanViewModel.class)
    public abstract ViewModel bindUserViewModel(ScanViewModel scanViewModel);

    @Binds
    @IntoMap
    @ViewModelKey(ConnectViewModel.class)
    public abstract ViewModel bindConnectViewModel(ConnectViewModel scanViewModel);

    @Binds
    public abstract ViewModelProvider.Factory bindViewModelFactory(ViewModelFactory factory);
}
