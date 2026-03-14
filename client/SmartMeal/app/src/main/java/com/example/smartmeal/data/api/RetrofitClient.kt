package com.example.smartmeal.data.api

import com.example.smartmeal.data.local.TokenManager
import com.example.smartmeal.feature.auth.data.api.AuthApi
import com.example.smartmeal.feature.auth.data.models.RefreshRequest
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8000/"
    private var tokenManager: TokenManager? = null

    fun init(manager: TokenManager) {
        tokenManager = manager
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(Interceptor { chain ->
                val original = chain.request()
                val token = tokenManager?.getAccessToken()
                if (token != null) {
                    val request = original.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                    chain.proceed(request)
                } else {
                    chain.proceed(original)
                }
            })
            .authenticator(object : Authenticator {
                override fun authenticate(route: Route?, response: Response): Request? {
                    synchronized(this) {
                        if (response.code == 401) {
                            val manager = tokenManager ?: return null
                            val refreshToken = manager.getRefreshToken() ?: return null
                            
                            // Check if token was already refreshed by another thread
                            val currentAccessToken = manager.getAccessToken()
                            val requestAccessToken = response.request.header("Authorization")?.removePrefix("Bearer ")
                            
                            if (currentAccessToken != requestAccessToken) {
                                return response.request.newBuilder()
                                    .header("Authorization", "Bearer $currentAccessToken")
                                    .build()
                            }

                            val refreshRetrofit = Retrofit.Builder()
                                .baseUrl(BASE_URL)
                                .addConverterFactory(GsonConverterFactory.create())
                                .build()
                            
                            val authApi = refreshRetrofit.create(AuthApi::class.java)
                            
                            return try {
                                val refreshResponse = kotlinx.coroutines.runBlocking {
                                    authApi.refreshToken(RefreshRequest(refreshToken))
                                }
                                
                                if (refreshResponse.isSuccessful) {
                                    val newAccess = refreshResponse.body()?.access ?: return null
                                    val currentRefresh = manager.getRefreshToken() ?: refreshToken
                                    manager.saveTokens(newAccess, currentRefresh)
                                    
                                    response.request.newBuilder()
                                        .header("Authorization", "Bearer $newAccess")
                                        .build()
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }
                    return null
                }
            })
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }

    // Deprecated but kept for compatibility during migration if needed
    fun <T> createService(serviceClass: Class<T>, manager: TokenManager? = null): T {
        manager?.let { init(it) }
        return createService(serviceClass)
    }
}
