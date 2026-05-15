package ru.bippbupp.fv3chat.data.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

private const val BASE_URL = "https://faerytea.name/"

object NetworkModule {
    fun createApi(session: AuthSession): ChatApi {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                session.token?.takeIf { it.isNotBlank() }?.let { token ->
                    requestBuilder.header("X-Auth-Token", token)
                }
                chain.proceed(requestBuilder.build())
            }
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ChatApi::class.java)
    }

    fun thumbUrl(path: String): String = BASE_URL + "thumb/" + path.trimStart('/')

    fun imageUrl(path: String): String = BASE_URL + "img/" + path.trimStart('/')
}
