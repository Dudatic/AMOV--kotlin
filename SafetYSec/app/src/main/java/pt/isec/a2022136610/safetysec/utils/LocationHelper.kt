package pt.isec.a2022136610.safetysec.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*

class LocationHelper(context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(onLocationResult: (Location) -> Unit) {

        Log.d("GPS_DEBUG", "A tentar iniciar atualizações de GPS...")

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    // Agora enviamos o objeto Location completo para ter acesso à velocidade e tempo
                    Log.d("GPS_DEBUG", "Nova localização: Lat=${location.latitude}, Speed=${location.speed}")
                    onLocationResult(location)
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