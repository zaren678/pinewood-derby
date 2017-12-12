package com.johnanderson.pinewoodderbyiot.di;

import com.johnanderson.pinewoodderbyiot.MainActivity;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class MainActivityModule {

    @ContributesAndroidInjector
    abstract MainActivity contributeMainActivity();
}
