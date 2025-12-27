package pt.isec.a2022136610.safetysec.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

class FallDetector(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Callback para avisar o UI quando cair
    private var onFallDetected: (() -> Unit)? = null

    // Controlo para não disparar 10 vezes na mesma queda
    private var lastFallTime: Long = 0
    private val COOLDOWN_MS = 5000 // Espera 5 segundos entre deteções

    // Limite de força para considerar queda (aprox 2.5G)
    // O valor normal parado é ~9.8 m/s² (1G). Uma queda gera picos de 20-30.
    private val FALL_THRESHOLD = 25.0

    fun startListening(callback: () -> Unit) {
        if (accelerometer == null) {
            Log.e("FALL_DETECTOR", "Este dispositivo não tem acelerómetro!")
            return
        }
        onFallDetected = callback
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        Log.d("FALL_DETECTOR", "Deteção de quedas ATIVADA.")
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        Log.d("FALL_DETECTOR", "Deteção de quedas PARADA.")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Cálculo da magnitude do vetor (Força total)
            val acceleration = sqrt((x * x + y * y + z * z).toDouble())

            // Se a força for muito grande (Impacto)
            if (acceleration > FALL_THRESHOLD) {
                val currentTime = System.currentTimeMillis()

                // Verifica se já passou o tempo de cooldown
                if (currentTime - lastFallTime > COOLDOWN_MS) {
                    lastFallTime = currentTime
                    Log.w("FALL_DETECTOR", "QUEDA DETETADA! Força: $acceleration")

                    // Dispara o alerta
                    onFallDetected?.invoke()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Não precisamos de lidar com isto
    }
}