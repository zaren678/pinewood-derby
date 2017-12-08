package com.johnanderson.pinewoodderbyapp.di;

import com.johnanderson.pinewoodderbyapp.MainActivity;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class MainActivityModule {

    @ContributesAndroidInjector(modules = FragmentBuilderModule.class)
    abstract MainActivity contributeMainActivity();
}
