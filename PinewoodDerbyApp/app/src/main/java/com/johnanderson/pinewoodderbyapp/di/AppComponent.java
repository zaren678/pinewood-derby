package com.johnanderson.pinewoodderbyapp.di;

import android.app.Application;

import com.johnanderson.pinewoodderbyapp.MyApplication;
import com.johnanderson.pinewoodderbyapp.connect.ConnectActivity;
import com.johnanderson.pinewoodderbyapp.connect.ConnectViewModel;
import com.johnanderson.pinewoodderbyapp.scan.ScanActivity;
import com.johnanderson.pinewoodderbyapp.scan.ScanViewModel;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;
import dagger.android.support.DaggerApplication;

@Singleton
@Component(modules = {AndroidSupportInjectionModule.class, AppModule.class, ActivityModule.class})
public interface AppComponent extends AndroidInjector<DaggerApplication> {
    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder application(final Application application);
        AppComponent build();
    }
    void inject(DaggerApplication application);
    void inject(ScanViewModel scanViewModel);
    void inject(ScanActivity scanActivity);
    void inject(ConnectViewModel connectViewModel);
    void inject(ConnectActivity connectActivity);
}
