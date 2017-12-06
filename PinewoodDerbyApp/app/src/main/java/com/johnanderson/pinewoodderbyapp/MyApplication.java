package com.johnanderson.pinewoodderbyapp;

import com.johnanderson.pinewoodderbyapp.di.AppComponent;
import com.johnanderson.pinewoodderbyapp.di.AppInjector;
import com.johnanderson.pinewoodderbyapp.di.DaggerAppComponent;

import dagger.android.AndroidInjector;
import dagger.android.support.DaggerApplication;

/**
 * Created by johnanderson on 12/4/17.
 */

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
        AppInjector.INSTANCE.init(appComponent, this);
    }
}
