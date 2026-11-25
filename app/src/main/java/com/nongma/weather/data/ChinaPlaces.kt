package com.nongma.weather.data

import android.content.Context
import com.nongma.weather.City
import com.nongma.weather.R

/** Parses the bundled China place index (name|lat|lon|region per line) once. */
internal object ChinaPlaces {
    @Volatile
    private var cached: List<City>? = null

    fun load(context: Context): List<City> {
        cached?.let { return it }
        val list = context.resources.openRawResource(R.raw.china_places).bufferedReader().useLines { lines ->
            lines.mapNotNull { line ->
                val parts = line.split('|')
                if (parts.size < 3) return@mapNotNull null
                val lat = parts[1].toDoubleOrNull() ?: return@mapNotNull null
                val lon = parts[2].toDoubleOrNull() ?: return@mapNotNull null
                City(parts[0], lat, lon, parts.getOrNull(3).orEmpty())
            }.toList()
        }
        cached = list
        return list
    }
}
