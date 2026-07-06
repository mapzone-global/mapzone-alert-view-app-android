package com.mapzone.mapzonealertview.domain.repository

import com.mapzone.mapzonealertview.domain.model.AutocompleteItem
import com.mapzone.mapzonealertview.domain.model.PlaceResult
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface SearchApi {
    @GET("api/autocomplete/v4")
    suspend fun autocomplete(
        @Query("apikey") apikey: String,
        @Query("text") text: String,
        @Query("focus") focus: String?,
        @Query("display_type") displayType: Int = 6,
    ): List<AutocompleteItem>

    @GET("api/place/v4")
    suspend fun place(
        @Query("apikey") apikey: String,
        @Query("refid") refid: String,
    ): PlaceResult

    companion object {
        fun create(): SearchApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                })
                .build()

            return Retrofit.Builder()
                .baseUrl("https://maps.vietmap.vn/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SearchApi::class.java)
        }
    }
}
