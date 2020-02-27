package pw.lowee.mvisample.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pw.lowee.mvisample.BuildConfig
import pw.lowee.mvisample.EitherE
import pw.lowee.mvisample.io
import pw.lowee.mvisample.models.GifObject
import pw.lowee.mvisample.models.GiphyResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiClient {

    @GET("/v1/gifs/search")
    suspend fun search(
        @Query("api_key") apiKey: String,
        @Query("q") query: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): GiphyResponse<List<GifObject>>

}

interface DataSource {
    suspend fun search(query: String, offset: Int, limit: Int): EitherE<List<GifObject>>
}

class DataSourceImpl(
    private val client: ApiClient
) : DataSource {

    override suspend fun search(query: String, offset: Int, limit: Int) = io {
        client.search(BuildConfig.API_KEY, query, offset, limit).data
    }

}

fun buildClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .build()
}