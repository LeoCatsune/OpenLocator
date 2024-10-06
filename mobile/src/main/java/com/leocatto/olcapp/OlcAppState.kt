package com.leocatto.olcapp

import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.openlocationcode.OpenLocationCode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class OlcAppState {
    private val logName = "OlcAppState"

    private val parent: AppCompatActivity

    var isLocationPrecise: Boolean = false
    var locationUpdatesRunning: Boolean = false

    var fullLocationCode: String? = null
        set(value) {
            // Don't allow un-setting, if already set.
            if (value == null) return

            field = value
            regionPart = value.substring(0, 4)
            areaPart = value.substring(4, 8)
            plusPart = value.substring(8, 11)
        }

    var regionPart = "XXXX"
    var areaPart = "YYYY"
    var plusPart = "+ZZ"

    private val regionView: TextView
    private val areaView: TextView
    private val plusView: TextView
    private val lastUpdated: TextView
    private val approxLabel: TextView
    private val refreshLayout: SwipeRefreshLayout

    val locationRequest: LocationRequest
    val locationCallback: LocationCallback

    constructor(
        parent: AppCompatActivity,
        region: TextView,
        area: TextView,
        plus: TextView,
        updated: TextView,
        approx: TextView,
        refresh: SwipeRefreshLayout
    ) {
        this.parent = parent
        regionView = region
        areaView = area
        plusView = plus
        lastUpdated = updated
        approxLabel = approx
        refreshLayout = refresh

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000L)
            .setMinUpdateIntervalMillis(5000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationAvailability(p0: LocationAvailability) {
                Log.d(logName, "OnLocationAvailability Callback: ${p0.isLocationAvailable}")
            }

            override fun onLocationResult(p0: LocationResult) {
                if (p0.locations.isEmpty()) {
                    Log.w(logName, "OnLocationResult Callback with No Locations?")
                    return
                }

                val location = p0.locations.last()
                val olc = OpenLocationCode.encode(location.latitude, location.longitude)

                Log.d(
                    logName,
                    "OnLocationResult Callback: ${location.latitude}, ${location.longitude}"
                )
                Log.d(logName, "Generated OpenLocationCode: $olc")

                updateAll(olc)
            }
        }
    }

    fun updateAll(olc: String) {
        fullLocationCode = olc
        updateViews()
    }

    fun updateViews() {
        approxLabel.visibility = if (isLocationPrecise) View.GONE else View.VISIBLE

        regionView.text = regionPart
        areaView.text = areaPart
        plusView.text = plusPart

        val currentTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd 'at' HH:mm:ss")
        val formattedDate = currentTime.format(formatter)
        val lastUpdatedMessage = parent.getString(R.string.last_updated, formattedDate)

        lastUpdated.text = lastUpdatedMessage

        refreshLayout.isRefreshing = false
    }
}