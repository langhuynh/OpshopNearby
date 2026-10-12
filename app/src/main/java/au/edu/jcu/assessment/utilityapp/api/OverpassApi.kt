package au.edu.jcu.assessment.utilityapp.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Same idea as the QuoteApi in the template:
//   QuoteApi:    https://zenquotes.io/ + api/quotes
//   OverpassApi: https://overpass-api.de/ + api/interpreter + ?data=<question>

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

// RetrofitInstance is an object that creates a single, static instance (a singleton)
object RetrofitInstance {

    // EXTRA 1: OpenStreetMap asks every app to send its name (User-Agent),
    // and we stop waiting for a slow server after 15 seconds.
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", "ThriftFinder/1.0").build())
        }
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Exactly like the template's "val api", but for any base URL.
    private fun create(baseUrl: String): OverpassApi =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OverpassApi::class.java)

    // EXTRA 2: the template has 1 server; we have 3 free servers because they are often busy.
    // ShopViewModel tries them in order until one answers.
    val servers: List<OverpassApi> by lazy { // defers the initialization of the variable until it is first accessed.
        listOf(
            create("https://overpass-api.de/"),
            create("https://overpass.private.coffee/"),
            create("https://maps.mail.ru/osm/tools/overpass/")
        )
    }
}