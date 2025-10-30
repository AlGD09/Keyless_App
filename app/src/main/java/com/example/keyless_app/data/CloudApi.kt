package com.example.keyless_app.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Path

interface CloudApi {
    @POST("api/devices/request")
    suspend fun requestToken(@Body payload: TokenRequest): Response<TokenResponse>

    @GET("api/devices/rcus/{deviceId}")
    suspend fun requestRcus(
        @Path("deviceId") deviceId: String
    ): Response<List<RcuResponse>>
}
