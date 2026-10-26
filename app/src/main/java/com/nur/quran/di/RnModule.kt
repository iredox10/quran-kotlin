package com.nur.quran.di

import android.content.Context
import com.nur.quran.data.api.rn.RnAuthApi
import com.nur.quran.data.api.rn.RnAuthClient
import com.nur.quran.data.api.rn.RnBackend
import com.nur.quran.data.api.rn.RnPrefsTokenStore
import com.nur.quran.data.api.rn.RnTokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * Hilt wiring for the Render.com backend (quran-nur-api). The Rn graph is
 * built even when the migration flag is OFF: RnBackend.isRenderApiEnabled
 * decides at runtime which transport the consumers use, and the Rn
 * singletons wait unused. The /auth surface is deliberately registered
 * WITHOUT the bearer interceptor and WITHOUT the 401 authenticator — a
 * failed refresh must 401 out, never recurse.
 */
@Module
@InstallIn(SingletonComponent::class)
object RnModule {

    @Provides
    @Singleton
    fun provideRnBackend(): RnBackend = RnBackend()

    @Provides
    @Singleton
    fun provideRnTokenStore(@ApplicationContext context: Context): RnTokenStore =
        RnPrefsTokenStore(context)

    /** /auth/* surface — plain client, no auth headers, no 401 authenticator. */
    @Provides
    @Singleton
    fun provideRnAuthApi(backend: RnBackend, okHttpClient: OkHttpClient): RnAuthApi =
        Retrofit.Builder()
            .baseUrl(backend.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RnAuthApi::class.java)

    @Provides
    @Singleton
    fun provideRnAuthClient(api: RnAuthApi, tokenStore: RnTokenStore): RnAuthClient =
        RnAuthClient(api, tokenStore)
}
