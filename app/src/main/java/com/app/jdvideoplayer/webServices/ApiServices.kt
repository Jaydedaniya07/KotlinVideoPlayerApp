package com.app.jdvideoplayer.webServices

import com.app.jdvideoplayer.model.DemoClass
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiServices {

    @POST("user/login")
    fun login(@Body hashMap: HashMap<String, Any>): Call<DemoClass>

    @GET("user/data")
    fun getData(@Body hashMap: HashMap<String, Any>): Call<DemoClass>
}