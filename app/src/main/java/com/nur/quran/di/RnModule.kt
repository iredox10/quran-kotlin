package com.nur.quran.di

import android.content.Context
import com.nur.quran.data.api.rn.RnApi
import com.nur.quran.data.api.rn.RnApiClient
import com.nur.quran.data.api.rn.RnAuthApi
import com.nur.quran.data.api.rn.RnAuthClient
import com.nur.quran.data.api.rn.RnAuthenticator
import com.nur.quran.data.api.rn.RnAuthInterceptor
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
 * Heroku transport wiring. Authentication uses the plain client so a rejected
 * refresh cannot recurse through the authenticated content/sync client.
 */
@Module
@InstallIn(SingletonComponent::class)
object RnModule {

    @Provides
    @Singleton
    fun provideRnBackend(): RnBackend = RnBackend()

    @Provides
    @Singleton
    fun provideRnTokenStore(@ApplicationContext context: Context, backend: RnBackend): RnTokenStore =
        RnPrefsTokenStore(context, backend)

    /** Auth surface (register/login/refresh) — plain client, no auth headers, no 401 authenticator. */
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

    /** Content + sync surface — Bearer via interceptor, refresh-on-401 replay. */
    @Provides
    @Singleton
    fun provideRnApi(
        backend: RnBackend,
        okHttpClient: OkHttpClient,
        authClient: RnAuthClient,
        tokenStore: RnTokenStore,
    ): RnApi {
        val client = okHttpClient.newBuilder()
            .addInterceptor(RnAuthInterceptor(tokenStore))
            .authenticator(RnAuthenticator(authClient))
            .build()
        return Retrofit.Builder()
            .baseUrl(backend.baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RnApi::class.java)
    }

    @Provides
    @Singleton
    fun provideRnApiClient(
        backend: RnBackend,
        api: RnApi,
        authClient: RnAuthClient,
        tokenStore: RnTokenStore,
    ): RnApiClient = RnApiClient(backend, api, authClient, tokenStore)
}
