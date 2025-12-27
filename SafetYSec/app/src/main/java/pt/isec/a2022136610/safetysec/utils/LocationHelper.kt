package pt.isec.a2022136610.safetysec.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import com.google.firebase.firestore.GeoPoint

class LocationHelper(context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(onLocationResult: (GeoPoint) -> Unit) {

        Log.d("GPS_DEBUG", "A tentar iniciar atualizações de GPS...")

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    Log.d("GPS_DEBUG", "Nova localização recebida: Lat=${location.latitude}, Long=${location.longitude}")
                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                    onLocationResult(geoPoint)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        ).addOnSuccessListener {
            Log.d("GPS_DEBUG", "Pedido de localização aceite pelo sistema!")
        }.addOnFailureListener { e ->
            Log.e("GPS_DEBUG", "Erro ao pedir localização: ${e.message}")
        }
    }

    fun stopLocationUpdates() {
        Log.d("GPS_DEBUG", "A parar GPS...")
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }
}