package com.nongma.weather.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timezone: String = "",
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int = 0,
    @SerialName("current_units") val currentUnits: CurrentUnits? = null,
    val current: CurrentWeather? = null,
    @SerialName("hourly_units") val hourlyUnits: HourlyUnits? = null,
    val hourly: HourlyWeather? = null,
    @SerialName("daily_units") val dailyUnits: DailyUnits? = null,
    val daily: DailyWeather? = null
)

@Serializable
data class CurrentUnits(
    val temperature: String = "°C",
    val humidity: String = "%",
    val windSpeed: String = "km/h"
)

@Serializable
data class CurrentWeather(
    val time: String = "",
    @SerialName("temperature_2m") val temperature: Double = 0.0,
    @SerialName("relative_humidity_2m") val relativeHumidity: Int = 0,
    @SerialName("apparent_temperature") val apparentTemperature: Double = 0.0,
    @SerialName("weather_code") val weatherCode: Int = 0,
    @SerialName("precipitation") val precipitation: Double = 0.0,
    @SerialName("cloud_cover") val cloudCover: Int = 0,
    @SerialName("pressure_msl") val pressureMsl: Double = 0.0,
    @SerialName("wind_speed_10m") val windSpeed: Double = 0.0,
    @SerialName("wind_direction_10m") val windDirection: Int = 0,
    @SerialName("is_day") val isDay: Int = 1
)

@Serializable
data class HourlyUnits(
    val temperature: String = "°C",
    @SerialName("precipitation_probability") val precipitationProbability: String = "%",
    @SerialName("wind_speed_10m") val windSpeed: String = "km/h"
)

@Serializable
data class HourlyWeather(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature: List<Double> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int> = emptyList(),
    @SerialName("is_day") val isDay: List<Int> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbability: List<Int?> = emptyList(),
    @SerialName("wind_speed_10m") val windSpeed: List<Double?> = emptyList()
)

@Serializable
data class DailyUnits(
    val temperatureMax: String = "°C",
    val temperatureMin: String = "°C",
    val precipitation: String = "mm"
)

@Serializable
data class DailyWeather(
    val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int> = emptyList(),
    @SerialName("temperature_2m_max") val temperatureMax: List<Double> = emptyList(),
    @SerialName("temperature_2m_min") val temperatureMin: List<Double> = emptyList(),
    @SerialName("precipitation_sum") val precipitationSum: List<Double> = emptyList(),
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int?> = emptyList(),
    val sunrise: List<String?> = emptyList(),
    val sunset: List<String?> = emptyList(),
    @SerialName("uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
    @SerialName("wind_speed_10m_max") val windSpeedMax: List<Double?> = emptyList(),
    @SerialName("wind_speed_10m_min") val windSpeedMin: List<Double?> = emptyList(),
    @SerialName("wind_gusts_10m_max") val windGustsMax: List<Double?> = emptyList()
)
