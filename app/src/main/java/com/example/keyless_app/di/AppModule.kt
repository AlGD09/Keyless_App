package com.example.keyless_app.di

import com.example.keyless_app.data.CloudClient
import com.example.keyless_app.data.BLEManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCloudClient(): CloudClient = CloudClient()

    @Provides
    @Singleton
    fun provideBLEManager(): BLEManager = BLEManager()
}
