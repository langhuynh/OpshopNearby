package au.edu.jcu.assessment.utilityapp.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// OverpassApi: POST https://overpass-api.de/api/interpreter   with the question as "data"
// The Overpass servers ask apps to send the question with POST (in the body),
// not in the link, and may block link (GET) requests when they are busy.

interface OverpassApi {
    @FormUrlEncoded                 // send the question like a web form...
    @POST("api/interpreter")        //to <server>/api/interpreter
    // non-blocking function to perform asynchronous, long-running operations
    suspend fun search(@Field("data") query: String): OverpassResponse
}

object OverpassQuery {
    fun opShopsAround(lat: Double, lon: Double, radiusMeters: Int): String =
        "[out:json][timeout:25];" +
                "nwr[\"shop\"~\"^(charity|second_hand)$\"](around:$radiusMeters,$lat,$lon);" +
                "out center tags;"
}

// RetrofitInstance is an object that creates a single, static instance (a singleton)
object RetrofitInstance {

    //OpenStreetMap asks every app to send a clear name (User-Agent),
      private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "ThriftFinder/1.0 (JCU CP3406 student project, Android)")
                .build()
            chain.proceed(request)
        }
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    // Exactly like the template's "val api", but for any base URL.
    private fun create(baseUrl: String): OverpassApi =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OverpassApi::class.java)

   // ShopViewModel tries them a few seconds apart and uses the first answer.
    // The addresses of the 2 free servers (also shown in the error message if they all fail).
    val urls = listOf(
        "https://overpass-api.de/",
        "https://overpass.private.coffee/"
    )

    // One OverpassApi for each address.
    val servers: List<OverpassApi> by lazy { // defers the initialization of the variable until it is first accessed.
        urls.map { url -> create(url) }
    }

    // The BACKUP: Nominatim (see NominatimApi.kt). Same app name (User-Agent) as above.
    val nominatim: NominatimApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NominatimApi::class.java)
    }
}