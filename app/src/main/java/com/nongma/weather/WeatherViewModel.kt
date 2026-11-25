package com.nongma.weather

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nongma.weather.data.CityRepository
import com.nongma.weather.data.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class City(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val region: String = ""
)

class WeatherViewModel(
    application: Application,
    private val repository: WeatherRepository = WeatherRepository(),
    private val cityRepository: CityRepository = CityRepository(application)
) : AndroidViewModel(application) {

    /** Cities the user added themselves. The live location chip is separate from this list. */
    private val _userCities = MutableStateFlow(cityRepository.getSavedCities())
    val userCities: StateFlow<List<City>> = _userCities.asStateFlow()

    private val _selectedCity = MutableStateFlow(cityRepository.getLastCity() ?: DEFAULT_CITY)
    val selectedCity: StateFlow<City> = _selectedCity.asStateFlow()

    /** True while the app is still showing its fallback city and waiting for a GPS fix. */
    val isAwaitingLocation: Boolean
        get() = _isFollowingLocation.value && _selectedCity.value == DEFAULT_CITY

    /**
     * When true the app shows the device's own location (fetched once when entering the
     * app); picking one of the user's cities turns it off, tapping the location chip turns
     * it back on.
     */
    private val _isFollowingLocation = MutableStateFlow(true)
    val isFollowingLocation: StateFlow<Boolean> = _isFollowingLocation.asStateFlow()

    private val _uiState: MutableStateFlow<WeatherUiState> =
        MutableStateFlow(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadWeather()
    }

    /** Selects one of the user's cities and leaves location-following mode. */
    fun selectCity(city: City) {
        _isFollowingLocation.value = false
        if (_selectedCity.value == city) return
        setSelected(city)
        loadWeather()
    }

    /** Adds a searched city, persists it, and selects it. */
    fun addCity(city: City) {
        val alreadyAdded = _userCities.value.any {
            it.name == city.name &&
                abs(it.latitude - city.latitude) < 0.001 &&
                abs(it.longitude - city.longitude) < 0.001
        }
        if (alreadyAdded) {
            selectCity(city)
            return
        }
        _userCities.value = _userCities.value + city
        cityRepository.saveCities(_userCities.value)
        selectCity(city)
    }

    /** Removes a city. If it was selected, the app goes back to the device location. */
    fun removeCity(city: City) {
        _userCities.value = _userCities.value.filter { it != city }
        cityRepository.saveCities(_userCities.value)
        if (_selectedCity.value == city) {
            setSelected(DEFAULT_CITY)
            _isFollowingLocation.value = true
            loadWeather()
        }
    }

    suspend fun searchCities(query: String): List<City> = cityRepository.search(query)

    fun resumeLocationFollowing() {
        _isFollowingLocation.value = true
    }

    /**
     * Feeds the single location fix obtained when entering the app (or after tapping the
     * location chip). Within a city area the fix only updates coordinates/name silently;
     * if the device actually moved the forecast is refreshed in the background.
     */
    fun onLocationFix(latitude: Double, longitude: Double, placeName: String?) {
        if (!_isFollowingLocation.value) return

        val previous = _selectedCity.value
        val previousIsUserCity = _userCities.value.contains(previous)
        val moved = abs(previous.latitude - latitude) >= 0.05 ||
            abs(previous.longitude - longitude) >= 0.05
        val name = placeName ?: if (previousIsUserCity) DEFAULT_LOCATION_NAME else previous.name

        if (previousIsUserCity || moved || previous == DEFAULT_CITY) {
            setSelected(City(name, latitude, longitude))
            // The city actually changed: show the loading state instead of leaving the
            // previous city's weather on screen while the new forecast downloads.
            loadWeather()
        } else if (
            previous.latitude != latitude ||
            previous.longitude != longitude ||
            previous.name != name
        ) {
            setSelected(City(name, latitude, longitude))
        }
    }

    fun refresh() {
        loadWeather(isRefresh = true)
    }

    private fun setSelected(city: City) {
        _selectedCity.value = city
        cityRepository.saveLastCity(city)
    }

    private fun loadWeather(isRefresh: Boolean = false) {
        val city = _selectedCity.value
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (isRefresh) {
                _isRefreshing.value = true
            } else {
                _uiState.value = WeatherUiState.Loading
            }
            try {
                val weather = repository.getForecast(city.latitude, city.longitude)
                _uiState.value = WeatherUiState.Success(weather = weather, cityName = city.name)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (!isRefresh || _uiState.value !is WeatherUiState.Success) {
                    _uiState.value = WeatherUiState.Error(
                        e.localizedMessage ?: "加载天气失败，请检查网络连接"
                    )
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private companion object {
        const val DEFAULT_LOCATION_NAME = "当前位置"
        val DEFAULT_CITY = City(DEFAULT_LOCATION_NAME, 39.9042, 116.4074)
    }
}
