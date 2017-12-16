package com.johnanderson.pinewoodderbyiot.di;

import android.app.Application;

import com.johnanderson.pinewoodderbyiot.board.BoardDefaultsModule;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;
import dagger.android.support.DaggerApplication;

@Singleton
@Component(modules = {
        AndroidSupportInjectionModule.class,
        AppModule.class,
        MainActivityModule.class,
        BoardDefaultsModule.class
})
public interface AppComponent extends AndroidInjector<DaggerApplication> {
    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder application(final Application application);
        AppComponent build();
    }
    void inject(DaggerApplication application);
}
