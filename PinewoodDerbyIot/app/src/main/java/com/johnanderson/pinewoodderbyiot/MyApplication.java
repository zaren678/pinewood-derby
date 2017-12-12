package com.johnanderson.pinewoodderbyiot;

import com.johnanderson.pinewoodderbyiot.di.AppComponent;
import com.johnanderson.pinewoodderbyiot.di.AppInjector;
import com.johnanderson.pinewoodderbyiot.di.DaggerAppComponent;

import dagger.android.AndroidInjector;
import dagger.android.support.DaggerApplication;
import timber.log.Timber;

public class MyApplication extends DaggerApplication {

    private AppComponent appComponent;

    public MyApplication() {
        super();
        appComponent = DaggerAppComponent.builder().application(this).build();
    }

    @Override
    protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
        return appComponent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
        AppInjector.INSTANCE.init(appComponent, this);
    }
}
