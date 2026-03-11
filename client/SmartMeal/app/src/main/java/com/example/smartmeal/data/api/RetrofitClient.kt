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

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private fun createHttpClient(tokenManager: TokenManager?): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(logging)

        if (tokenManager != null) {
            builder.addInterceptor(Interceptor { chain ->
                val original = chain.request()
                val token = tokenManager.getAccessToken()
                if (token != null) {
                    val request = original.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .method(original.method, original.body)
                        .build()
                    chain.proceed(request)
                } else {
                    chain.proceed(original)
                }
            })

            builder.authenticator(object : Authenticator {
                override fun authenticate(route: Route?, response: Response): Request? {
                    if (response.code == 401) {
                        val refreshToken = tokenManager.getRefreshToken() ?: return null
                        
                        // We need a separate Retrofit instance for refreshing to avoid infinite loops
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
                                val currentRefresh = tokenManager.getRefreshToken() ?: refreshToken
                                tokenManager.saveTokens(newAccess, currentRefresh)
                                
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
                    return null
                }
            })
        }

        return builder.build()
    }

    private var retrofit: Retrofit? = null

    private fun getRetrofit(tokenManager: TokenManager?): Retrofit {
        if (retrofit == null) {
            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(createHttpClient(tokenManager))
                .build()
        }
        return retrofit!!
    }

    fun <T> createService(serviceClass: Class<T>, tokenManager: TokenManager? = null): T {
        return getRetrofit(tokenManager).create(serviceClass)
    }
}
