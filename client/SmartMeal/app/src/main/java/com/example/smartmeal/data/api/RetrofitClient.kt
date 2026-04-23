package com.example.smartmeal.data.api

import com.example.smartmeal.data.local.TokenManager
import com.example.smartmeal.feature.auth.data.api.AuthApi
import com.example.smartmeal.feature.auth.data.models.RefreshRequest
import kotlinx.coroutines.runBlocking
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
    private const val BASE_URL = "https://approximate-restaurant-syracuse-york.trycloudflare.com/"
    private var tokenManager: TokenManager? = null

    fun init(manager: TokenManager) {
        tokenManager = manager
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val original = chain.request()
                // Не добавляем токен к запросам авторизации (login/register/refresh)
                val path = original.url.encodedPath
                if (path.contains("/token/") || path.contains("/register/")) {
                    return@addInterceptor chain.proceed(original)
                }

                val token = tokenManager?.getAccessToken()
                val request = if (token != null) {
                    original.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    original
                }
                chain.proceed(request)
            }
            .authenticator { _, response ->
                // Если запрос к самому refresh упал (401 или 500) — всё, приехали, выходим
                if (response.request.url.encodedPath.contains("/token/refresh/")) {
                    tokenManager?.clearTokens()
                    return@authenticator null
                }

                synchronized(this) {
                    val manager = tokenManager ?: return@authenticator null
                    val refreshToken = manager.getRefreshToken() ?: return@authenticator null

                    // 1. Проверяем, не обновил ли токен другой параллельный поток
                    val currentAccess = manager.getAccessToken()
                    val requestAccess = response.request.header("Authorization")?.removePrefix("Bearer ")
                    if (!currentAccess.isNullOrEmpty() && currentAccess != requestAccess) {
                        return@authenticator response.request.newBuilder()
                            .header("Authorization", "Bearer $currentAccess")
                            .build()
                    }

                    // 2. Пытаемся обновиться
                    val refreshRetrofit = Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()

                    val authApi = refreshRetrofit.create(AuthApi::class.java)

                    return@authenticator try {
                        val refreshResponse = runBlocking {
                            authApi.refreshToken(RefreshRequest(refreshToken))
                        }

                        if (refreshResponse.isSuccessful) {
                            val body = refreshResponse.body()
                            val newAccess = body?.access ?: throw Exception("Empty body")
                            val newRefresh = body.refresh ?: refreshToken // Поддержка ротации
                            manager.saveTokens(newAccess, newRefresh)

                            response.request.newBuilder()
                                .header("Authorization", "Bearer $newAccess")
                                .build()
                        } else {
                            // Если сервер ответил 401/400/500 — токен мертв или пользователь удален
                            manager.clearTokens()
                            null
                        }
                    } catch (e: Exception) {
                        // Ошибка сети или серьезный сбой сервера — сбрасываем всё
                        manager.clearTokens()
                        null
                    }
                }
            }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    fun <T> createService(serviceClass: Class<T>): T = retrofit.create(serviceClass)

    fun <T> createService(serviceClass: Class<T>, manager: TokenManager? = null): T {
        manager?.let { init(it) }
        return createService(serviceClass)
    }
}
