package com.example.keyless_app.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface CloudApi {
    @POST("api/devices/request")
    suspend fun requestToken(@Body payload: TokenRequest): Response<TokenResponse>
}
