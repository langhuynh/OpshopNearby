package au.edu.jcu.assessment.utilityapp.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface OverpassApi {
    @GET("api/interpreter")
    // non-blocking function to perform asynchronous, long-running operations
    // @Query("data") adds ?data=<query> to the end of the link
    suspend fun search(@Query("data") query: String): OverpassResponse
}

// The question that goes after ?data= :
// "find charity and second-hand shops within radiusMeters of lat, lon, and answer in JSON"
object OverpassQuery {
    fun opShopsAround(lat: Double, lon: Double, radiusMeters: Int): String =
        "[out:json][timeout:25];" +
                "nwr[\"shop\"~\"^(charity|second_hand)$\"](around:$radiusMeters,$lat,$lon);" +
                "out center tags;"
}
object RetrofitInstance {

    // EXTRA 1: OpenStreetMap asks every app to send its name (User-Agent),
    // and stop waiting for a slow server after 15 seconds.
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", "ThriftFinder/1.0").build())
        }
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun create(baseUrl: String): OverpassApi =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OverpassApi::class.java)

     // ShopViewModel tries them in order until one answers.
    val servers: List<OverpassApi> by lazy { // defers the initialization of the variable until it is first accessed.
        listOf(
            create("https://overpass-api.de/"),
            create("https://maps.mail.ru/osm/tools/overpass/")
        )
    }
}