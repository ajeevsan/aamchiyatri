package com.amchiyatri.rider.di

import android.content.Context
import com.amchiyatri.rider.BuildConfig
import com.amchiyatri.rider.data.remote.DirectionsApi
import com.amchiyatri.rider.util.ApiKeys
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            // Proves to Google that this REST call really comes from this app, so an
            // "Android apps" restricted API key (the kind the Maps/Places SDKs already need)
            // also works for the plain Directions REST endpoint. See:
            // https://developers.google.com/maps/api-security-best-practices
            val request = chain.request().newBuilder()
                .addHeader("X-Android-Package", context.packageName)
                .addHeader("X-Android-Cert", ApiKeys.androidCertSha1(context))
                .build()
            chain.proceed(request)
        }
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl("https://maps.googleapis.com/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideDirectionsApi(retrofit: Retrofit): DirectionsApi = retrofit.create(DirectionsApi::class.java)
}
