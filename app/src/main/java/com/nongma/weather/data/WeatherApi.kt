package com.nongma.weather.data

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m," +
            "apparent_temperature,precipitation,weather_code,cloud_cover,pressure_msl," +
            "wind_speed_10m,wind_direction_10m,is_day",
        @Query("hourly") hourly: String = "temperature_2m,weather_code,is_day," +
            "precipitation_probability,wind_speed_10m",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min," +
            "precipitation_sum,precipitation_probability_max,sunrise,sunset,uv_index_max," +
            "wind_speed_10m_max,wind_speed_10m_min,wind_gusts_10m_max",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 10
    ): WeatherResponse
}
