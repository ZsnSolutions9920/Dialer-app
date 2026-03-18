package com.customdialer.app.data.api

import com.customdialer.app.data.model.RefreshRequest
import com.customdialer.app.util.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Production server URL
    var BASE_URL = "https://app.scalamatic.com/"

    private var tokenManager: TokenManager? = null
    private var retrofit: Retrofit? = null
    private var apiService: ApiService? = null

    fun init(tokenManager: TokenManager, baseUrl: String? = null) {
        this.tokenManager = tokenManager
        if (baseUrl != null) {
            BASE_URL = baseUrl
        }
        rebuild()
    }

    fun updateBaseUrl(url: String) {
        BASE_URL = url
        rebuild()
    }

    private fun rebuild() {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val token = tokenManager?.getToken()
            val request = if (token != null) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }

            var response = chain.proceed(request)

            // If 401, try refreshing token
            if (response.code == 401 && token != null) {
                response.close()
                val refreshToken = tokenManager?.getRefreshToken()
                if (refreshToken != null) {
                    val newToken = runBlocking { refreshAccessToken(refreshToken) }
                    if (newToken != null) {
                        tokenManager?.saveToken(newToken)
                        val newRequest = chain.request().newBuilder()
                            .addHeader("Authorization", "Bearer $newToken")
                            .build()
                        response = chain.proceed(newRequest)
                    }
                }
            }

            response
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit?.create(ApiService::class.java)
    }

    private suspend fun refreshAccessToken(refreshToken: String): String? {
        return try {
            val tempRetrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val tempApi = tempRetrofit.create(ApiService::class.java)
            // Try customer refresh first, then agent refresh as fallback
            val response = tempApi.customerRefresh(RefreshRequest(refreshToken))
            if (response.isSuccessful) {
                response.body()?.accessToken
            } else {
                val agentResponse = tempApi.refreshToken(RefreshRequest(refreshToken))
                if (agentResponse.isSuccessful) agentResponse.body()?.accessToken else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getApi(): ApiService {
        if (apiService == null) {
            throw IllegalStateException("RetrofitClient not initialized. Call init() first.")
        }
        return apiService!!
    }
}
