package com.example.prueba3.service

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class LugarServiceFactory {
    companion object {
        fun getBaseUrl():String {
            return "https://cataas.com"
        }
        fun getRetrofit(): Retrofit {
            val baseUrl = getBaseUrl()
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            val converterFactory = MoshiConverterFactory.create(moshi)
            val retrofit = Retrofit.Builder()
                .addConverterFactory( converterFactory )
                .baseUrl( baseUrl )
                .build()
            return retrofit
        }
        fun <T> getService(interfazService:Class<T>):T {
            val retrofit = getRetrofit()
            return retrofit.create( interfazService )
        }
        fun getLugarService():LugarService {
            val retrofit = getRetrofit()
            return getService( LugarService::class.java )
        }
    }

}