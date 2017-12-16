package com.johnanderson.pinewoodderbyiot.board;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static com.johnanderson.pinewoodderbyiot.board.BoardDefaultsFactoryKt.getBoardDefaults;

@Module
public abstract class BoardDefaultsModule {

    @Provides
    @Singleton
    public static BoardDefaults providesBoardDefaults(){
        return getBoardDefaults();
    }
}
