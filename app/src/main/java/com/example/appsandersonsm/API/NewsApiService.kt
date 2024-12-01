package com.example.appsandersonsm.API

import com.example.appsandersonsm.Modelo.NewsResponse
import retrofit2.http.GET
import retrofit2.http.Query


interface NewsApiService {
    @GET("everything")
    suspend fun getNews(
        @Query("q") query: String,
        @Query("apiKey") apiKey: String,
        @Query("language") language: String = "es", // Idioma por defecto inglés
        @Query("sortBy") sortBy: String = "relevancy" // Ordenar por relevancia
    ): NewsResponse
}
