package com.leocatto.olcapp

import android.Manifest.permission
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.snackbar.Snackbar
import com.google.openlocationcode.OpenLocationCode

class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var appState: OlcAppState

    lateinit var regionCode: TextView
    lateinit var areaCode: TextView
    lateinit var plusCode: TextView
    lateinit var lastUpdated: TextView
    lateinit var approxLabel: TextView
    lateinit var shareButton: Button
    lateinit var copyButton: Button
    lateinit var mapsButton: Button

    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.refreshLayout)

        regionCode = findViewById<TextView>(R.id.regionCode)
        areaCode = findViewById<TextView>(R.id.areaCode)
        plusCode = findViewById<TextView>(R.id.plusCode)
        lastUpdated = findViewById<TextView>(R.id.lastUpdated)
        approxLabel = findViewById<TextView>(R.id.approxLocationWarning)
        shareButton = findViewById<Button>(R.id.share)
        copyButton = findViewById<Button>(R.id.copy)
        mapsButton = findViewById<Button>(R.id.maps)

        appState = OlcAppState(
            this,
            regionCode,
            areaCode,
            plusCode,
            lastUpdated,
            approxLabel,
            swipeRefreshLayout
        )

        swipeRefreshLayout.setOnRefreshListener { refreshLocation() }
        shareButton.setOnClickListener {
            if (appState.fullLocationCode === null) return@setOnClickListener
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, appState.fullLocationCode)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(intent, null)
            startActivity(shareIntent)
        }
        copyButton.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    "OpenLocationCode",
                    appState.fullLocationCode
                )
            )
        }
        mapsButton.setOnClickListener {
            if(appState.fullLocationCode == null) return@setOnClickListener
            val gmmIntentUri = Uri.parse("http://plus.codes/${appState.fullLocationCode}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            if(mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            }
            else Toast.makeText(this, "App Not Installed", Toast.LENGTH_SHORT).show()
        }

        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(permission.ACCESS_FINE_LOCATION, false) -> {
                    // Precise Location...
                    Log.d(getString(R.string.app_name), "Got Precise Location Permission")
                    appState.isLocationPrecise = true
                    refreshLocation()
                }

                permissions.getOrDefault(permission.ACCESS_COARSE_LOCATION, false) -> {
                    // Coarse Location...
                    Log.d(getString(R.string.app_name), "Got Coarse Location Permission")
                    appState.isLocationPrecise = false
                    refreshLocation()
                }

                else -> {
                    // No Location Access
                    Log.w(getString(R.string.app_name), "Location Permission Rejected")
                    showPermissionSnackbar()
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

        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()

        refreshLocation()
    }

    private fun refreshLocation() {
        swipeRefreshLayout.isRefreshing = true

        if (ActivityCompat.checkSelfPermission(
                this,
                permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            swipeRefreshLayout.isRefreshing = false
            return showPermissionSnackbar()
        }

        snackbar?.dismiss()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location ->
            val olc = OpenLocationCode.encode(location.latitude, location.longitude)
            Log.d(getString(R.string.app_name), "Got OpenLocationCode: $olc")
            appState.updateAll(olc)
        }

        if (!appState.locationUpdatesRunning) startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (appState.locationUpdatesRunning) return
        if (ActivityCompat.checkSelfPermission(
                this,
                permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(getString(R.string.app_name), "Starting Location Update Handler")
            fusedLocationClient.requestLocationUpdates(
                appState.locationRequest,
                appState.locationCallback,
                mainLooper
            )
            appState.locationUpdatesRunning = true
        } else {
            Log.w(
                getString(R.string.app_name),
                "Cannot Start Location Update Handler: No Permission"
            )
        }
    }

    private fun stopLocationUpdates() {
        if (!appState.locationUpdatesRunning) return
        Log.d(getString(R.string.app_name), "Stopping Location Update Handler")
        fusedLocationClient.removeLocationUpdates(appState.locationCallback)
        appState.locationUpdatesRunning = false
    }

    private fun showPermissionSnackbar() {
        // We don't have location permission here - stop the location update process (if running)
        stopLocationUpdates()

        // Root view to attach the Snackbar to
        val rootView = findViewById<View>(android.R.id.content)

        snackbar = Snackbar.make(
            rootView,
            "Location permission is needed to show your location",
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction("Settings") {
                // Open the app's settings when the user clicks "Settings"
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
        snackbar!!.show()
    }
}