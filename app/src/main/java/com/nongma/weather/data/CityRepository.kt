package com.nongma.weather.data

import android.content.Context
import com.nongma.weather.City
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlin.math.roundToInt

@Serializable
data class GeocodingResponse(val results: List<GeocodingResult>? = null)

@Serializable
data class GeocodingResult(
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val country: String? = null,
    val admin1: String? = null
)

interface GeocodingApi {
    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "zh",
        @Query("format") format: String = "json"
    ): GeocodingResponse
}

/** Persists user cities and searches places online, with a China place index as fallback. */
class CityRepository(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("weather_cities", Context.MODE_PRIVATE)
    private val chinaPlaces: List<City> by lazy { ChinaPlaces.load(appContext) }

    private val api: GeocodingApi = Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/")
        .client(OkHttpClient.Builder().build())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(GeocodingApi::class.java)

    fun getSavedCities(): List<City> = try {
        val raw = prefs.getString(KEY_CITIES, null) ?: return emptyList()
        json.decodeFromString<List<City>>(raw)
    } catch (_: Exception) {
        emptyList()
    }

    fun saveCities(cities: List<City>) {
        prefs.edit().putString(KEY_CITIES, json.encodeToString(cities)).apply()
    }

    fun getLastCity(): City? = try {
        prefs.getString(KEY_LAST, null)?.let { json.decodeFromString<City>(it) }
    } catch (_: Exception) {
        null
    }

    fun saveLastCity(city: City) {
        prefs.edit().putString(KEY_LAST, json.encodeToString(city)).apply()
    }

    suspend fun search(name: String): List<City> {
        val keyword = name.trim()
        if (keyword.isEmpty()) return emptyList()

        // Local Chinese index: instant, works offline and covers county-level places.
        val local = searchLocal(keyword)

        // Online search (Open-Meteo): international cities and anything the index misses.
        // Open-Meteo indexes full Chinese names, so retry with 市/县/区 when needed.
        var online = searchOnline(keyword)
        if (online.isEmpty()) {
            for (suffix in arrayOf("市", "县", "区")) {
                online = searchOnline(keyword + suffix)
                if (online.isNotEmpty()) break
            }
        }

        return (local + online)
            .distinctBy { place ->
                place.name + "_" +
                    (place.latitude * 10).roundToInt() + "_" +
                    (place.longitude * 10).roundToInt()
            }
            .take(8)
    }

    /** Ranks exact matches, then prefix matches, then substring matches. */
    private fun searchLocal(keyword: String): List<City> {
        fun bare(name: String): String {
            var result = name
            for (suffix in arrayOf("市", "县", "区", "盟", "州", "地区", "林区")) {
                result = result.removeSuffix(suffix)
            }
            return result
        }
        return chinaPlaces.asSequence()
            .mapNotNull { place ->
                val score = when {
                    bare(place.name).equals(keyword, ignoreCase = true) -> 0
                    place.name.equals(keyword, ignoreCase = true) -> 0
                    bare(place.name).startsWith(keyword, ignoreCase = true) -> 1
                    place.name.contains(keyword, ignoreCase = true) -> 2
                    else -> return@mapNotNull null
                }
                score to place
            }
            .sortedBy { it.first }
            .map { it.second }
            .take(8)
            .toList()
    }

    private suspend fun searchOnline(keyword: String): List<City> = try {
        api.search(name = keyword).results.orEmpty()
            .filter { it.latitude.isFinite() && it.longitude.isFinite() }
            .map {
                City(
                    name = it.name,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    region = listOfNotNull(it.admin1, it.country)
                        .distinct()
                        .joinToString(" · ")
                )
            }
    } catch (_: Exception) {
        emptyList()
    }

    private companion object {
        const val KEY_CITIES = "cities"
        const val KEY_LAST = "last_city"
    }
}
