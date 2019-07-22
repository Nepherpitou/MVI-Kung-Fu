package pw.lowee.mvisample.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pw.lowee.mvisample.BuildConfig
import pw.lowee.mvisample.EitherE
import pw.lowee.mvisample.io
import pw.lowee.mvisample.models.GifObject
import pw.lowee.mvisample.models.GiphyResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiClient {

    @GET("/v1/gifs/search")
    fun search(
        @Query("api_key") apiKey: String,
        @Query("q") query: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): Call<GiphyResponse<List<GifObject>>>

}

interface DataSource {
    suspend fun search(query: String, offset: Int, limit: Int): EitherE<List<GifObject>>
}

class DataSourceImpl(
    private val client: ApiClient
) : DataSource {

    override suspend fun search(query: String, offset: Int, limit: Int) = io {
        val r = client.search(BuildConfig.API_KEY, query, offset, limit).execute()
        r?.body()?.data ?: badResponse()
    }

}

fun badResponse(message: String = "Bad Response"): Nothing {
    throw RuntimeException(message)
}

fun buildClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .build()
}