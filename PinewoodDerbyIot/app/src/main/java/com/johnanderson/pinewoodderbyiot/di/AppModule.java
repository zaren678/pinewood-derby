package com.johnanderson.pinewoodderbyiot.di;

import android.app.Application;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;

@Module
public abstract class AppModule {

    @Binds
    @ForApplication
    abstract Context bindContext(Application application);

    @Provides
    @Singleton
    public static BluetoothManager providesBluetoothManager(final Application application){
        return (BluetoothManager) application.getSystemService(Context.BLUETOOTH_SERVICE);
    }
}
