package com.nur.quran.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nur.quran.data.api.QuranApi
import com.nur.quran.data.db.QuranDatabase
import com.nur.quran.data.db.dao.QuranDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder().create()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideQuranApi(okHttpClient: OkHttpClient): QuranApi {
        return Retrofit.Builder()
            .baseUrl("https://api.quran.com/api/v4/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QuranApi::class.java)
    }

    @Provides
    @Singleton
    fun provideQuranDatabase(@ApplicationContext context: Context): QuranDatabase {
        return Room.databaseBuilder(
            context,
            QuranDatabase::class.java,
            "quran_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideQuranDao(database: QuranDatabase): QuranDao {
        return database.quranDao()
    }
}
