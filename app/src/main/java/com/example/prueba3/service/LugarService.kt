package com.example.prueba3.service

import com.example.prueba3.entity.Lugar
import retrofit2.http.GET

interface LugarService {
    @GET("/cat?json=true")
    suspend fun getLugarAleatorio(): Lugar
}