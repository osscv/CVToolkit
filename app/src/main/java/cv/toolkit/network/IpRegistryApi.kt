package cv.toolkit.network

import cv.toolkit.data.IpInfo
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface IpRegistryApi {

    @GET("api/")
    suspend fun getIpInfo(
        @Query("key") apiKey: String,
        @Query("ip") ip: String
    ): Response<IpInfo>

    @GET("api/")
    suspend fun getMyIpInfo(
        @Query("key") apiKey: String
    ): Response<IpInfo>

    companion object {
        const val BASE_URL = "https://ipinfo.dkly.net/"
        const val API_KEY = "85048f15d8349252d7ec19da71cae9ab7f9177fac57cda34854acee52b916dc9"
    }
}

