package com.example.keyless_app.di

import android.content.Context
import com.example.keyless_app.data.CloudClient
import com.example.keyless_app.data.BLEManager
import com.example.keyless_app.data.MainRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCloudClient(@ApplicationContext context: Context): CloudClient = CloudClient(context)

    @Provides
    @Singleton
    fun provideBLEManager(@ApplicationContext context: Context): BLEManager = BLEManager(context)

    @Provides
    @Singleton
    fun provideKeylessRepository(
        @ApplicationContext context: Context,
        cloudClient: CloudClient,
        bleManager: BLEManager
    ): MainRepository = MainRepository(context, cloudClient, bleManager)
}
