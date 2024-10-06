/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.leocatto.olcapp.presentation

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.android.gms.location.*
import com.google.openlocationcode.OpenLocationCode
import com.leocatto.olcapp.R
import com.leocatto.olcapp.presentation.theme.OpenLocatorTheme

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var appState: OlcAppState

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        appState = OlcAppState()

        setContent {
            WearApp(appState)
        }

        val hasGPS = packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
        Log.d(getString(R.string.app_name), "Onboard GPS Check: $hasGPS")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 30000L)
                .setMinUpdateIntervalMillis(5000L)
                .build()
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val olc = OpenLocationCode.encode(location.latitude, location.longitude)
                    Log.d(
                        getString(R.string.app_name),
                        "Got Location Update: ${location.latitude}, ${location.longitude}"
                    )
                    Log.d(getString(R.string.app_name), "Created OpenLocationCode: $olc")
                    appState.fullLocationCode = olc
                }
            }

            override fun onLocationAvailability(p0: LocationAvailability) {
                super.onLocationAvailability(p0)

                Log.d(
                    getString(R.string.app_name),
                    "Got LocationAvailability Update: ${p0.isLocationAvailable}"
                )
            }
        }

        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(permission.ACCESS_FINE_LOCATION, false) -> {
                    Log.d(getString(R.string.app_name), "Got Fine Location Permission")
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        mainLooper
                    )
                }

                permissions.getOrDefault(permission.ACCESS_COARSE_LOCATION, false) -> {
                    Log.d(getString(R.string.app_name), "Got Coarse Location Permission")
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        mainLooper
                    )
                }

                else -> {
                    Log.w(getString(R.string.app_name), "Location Permission Denied")
                }
            }
        }

        locationPermissionRequest.launch(
            arrayOf(
                permission.ACCESS_FINE_LOCATION,
                permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    override fun onPause() {
        super.onPause()
        Log.d(getString(R.string.app_name), "Pausing")
        fusedLocationClient.removeLocationUpdates(object : LocationCallback() {})
    }
}

@Composable
internal fun WearApp(state: OlcAppState) {
    val state = remember { mutableStateOf(state) }

    OpenLocatorTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            key(state.value.fullLocationCode) {
                Column {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.onSurfaceVariant,
                        fontSize = 24.sp,
                        text = state.value.regionPart
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary,
                        fontSize = 52.sp,
                        text = state.value.areaPart
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.onSurface,
                        fontSize = 32.sp,
                        text = state.value.plusPart
                    )
                }
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp(OlcAppState())
}