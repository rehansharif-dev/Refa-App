package com.example.transportapp.di

import com.example.transportapp.data.repository.AuthRepositoryImpl
import com.example.transportapp.data.repository.MapRepositoryImpl
import com.example.transportapp.data.repository.RideRepositoryImpl
import com.example.transportapp.domain.repository.AuthRepository
import com.example.transportapp.domain.repository.MapRepository
import com.example.transportapp.domain.repository.RideRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindMapRepository(
        mapRepositoryImpl: MapRepositoryImpl
    ): MapRepository

    @Binds
    @Singleton
    abstract fun bindRideRepository(
        rideRepositoryImpl: RideRepositoryImpl
    ): RideRepository
}