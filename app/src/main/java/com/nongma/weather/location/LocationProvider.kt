package com.nongma.weather.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * A single location reading together with a best-effort reverse geocoded place name.
 */
data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val placeName: String?
)

/**
 * Fetches the device location once — when the app is opened or when the user taps the
 * location chip. No continuous updates are requested.
 *
 * The cached last location is delivered immediately so switching back to the current
 * location is instant; a fresh fix is then requested in the background with a short
 * timeout and only delivered if it is meaningfully different.
 */
class LocationProvider(context: Context) {

    private val appContext = context.applicationContext
    private val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var inFlight = false
    private var freshInFlight = false
    private var lastDelivered: Location? = null

    // Reverse geocode cache.
    private var cachedName: String? = null

    private val hasPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                appContext, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

    /**
     * Requests a single location fix. Safe to call repeatedly; calls made while the fast
     * cached lookup is still in flight are ignored. Does nothing without permission.
     */
    fun fetchOnce(onFix: (LocationFix) -> Unit) {
        if (!hasPermission || inFlight) return
        inFlight = true
        // A new request always emits its first fix, even if it is the same spot as before:
        // the user may just have switched back here from another city.
        lastDelivered = null

        try {
            // Fast path: the system's cached position is usually good enough for weather.
            fusedClient.lastLocation
                .addOnSuccessListener { last ->
                    inFlight = false
                    if (last != null) deliver(last, onFix)
                    requestFreshFix(onFix)
                }
                .addOnFailureListener {
                    inFlight = false
                    requestFreshFix(onFix)
                }
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call.
            inFlight = false
        }
    }

    /** Slow path: a fresh fix, capped so a cold GPS can never block the UI. */
    private fun requestFreshFix(onFix: (LocationFix) -> Unit) {
        if (freshInFlight || !hasPermission) return
        freshInFlight = true

        try {
            val cancellation = CancellationTokenSource()
            Handler(Looper.getMainLooper()).postDelayed({
                cancellation.cancel()
            }, FRESH_FIX_TIMEOUT_MS)

            fusedClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellation.token
            ).addOnSuccessListener { current ->
                freshInFlight = false
                if (current != null) deliver(current, onFix)
            }.addOnFailureListener {
                freshInFlight = false
            }
        } catch (_: SecurityException) {
            freshInFlight = false
        }
    }

    /** Stops any pending geocoding work and releases the scope. */
    fun destroy() {
        inFlight = false
        freshInFlight = false
        scope.cancel()
    }

    private fun deliver(location: Location, onFix: (LocationFix) -> Unit) {
        // Ignore fresh fixes that are practically the same spot as the cached one.
        val previous = lastDelivered
        if (previous != null && previous.distanceTo(location) < 200f) return
        lastDelivered = location

        val latitude = location.latitude
        val longitude = location.longitude
        val knownName = cachedName ?: DEFAULT_PLACE_NAME

        // Emit immediately with whatever name we already know so weather can load at once.
        onFix(LocationFix(latitude, longitude, knownName))

        if (!Geocoder.isPresent()) return
        scope.launch {
            val name = reverseGeocode(latitude, longitude)
            if (name != null) {
                cachedName = name
                if (name != knownName) {
                    withContext(Dispatchers.Main) {
                        onFix(LocationFix(latitude, longitude, name))
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocode(latitude: Double, longitude: Double): String? {
        return try {
            val geocoder = Geocoder(appContext, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            val address = addresses?.firstOrNull() ?: return null
            address.locality
                ?: address.subAdminArea
                ?: address.adminArea
                ?: address.featureName
        } catch (_: Exception) {
            // Geocoder unavailable / offline / service error — fall back to the generic name.
            null
        }
    }

    private companion object {
        const val DEFAULT_PLACE_NAME = "当前位置"
        const val FRESH_FIX_TIMEOUT_MS = 5_000L
    }
}
