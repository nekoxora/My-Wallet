package com.example.mywallet.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("api_keuangan/insert_investasi.php")
    suspend fun simpanInvestasi(@Body data: InvestasiData): ApiResponse

    @POST("api_keuangan/delete_investasi.php")
    suspend fun hapusInvestasi(@Body data: DeleteData): ApiResponse

    @GET("api_keuangan/get_histori.php")
    suspend fun getHistori(@Query("device_id") deviceId: String): List<Transaksi>

    @GET("api_keuangan/get_berita_rss.php")
    suspend fun getBerita(): BeritaResponse
}

object RetrofitClient {
    private const val BASE_URL = "http://43.133.150.113/"
    val instance: ApiService by lazy {
        val gson = com.google.gson.GsonBuilder()
            .setLenient()
            .create()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}