package com.nongma.weather

import com.nongma.weather.data.WeatherResponse

sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data class Success(val weather: WeatherResponse, val cityName: String) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}
