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

    // Callbacks
    private var onFallDetected: (() -> Unit)? = null
    private var onAccidentDetected: (() -> Unit)? = null

    private var lastEventTime: Long = 0
    // Aumentei o cooldown para evitar disparos repetidos se o telemóvel continuar a abanar
    private val COOLDOWN_MS = 3000

    // --- AJUSTE DE SENSIBILIDADE
    private val FALL_THRESHOLD = 25.0

    // 40.0 (~4.5G) = Requer impacto muito forte (Simula acidente)
    private val ACCIDENT_THRESHOLD = 45.0

    fun startListening(onFall: () -> Unit, onAccident: () -> Unit) {
        if (accelerometer == null) {
            Log.e("FALL_DETECTOR", "Este dispositivo não tem acelerómetro!")
            return
        }
        this.onFallDetected = onFall
        this.onAccidentDetected = onAccident

        // Mantemos o GAME delay porque é o que permite detetar os picos rápidos
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        Log.d("FALL_DETECTOR", "Deteção de Quedas e Acidentes ATIVADA (Threshold: $FALL_THRESHOLD).")
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        Log.d("FALL_DETECTOR", "Deteção PARADA.")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val acceleration = sqrt((x * x + y * y + z * z).toDouble())
            val currentTime = System.currentTimeMillis()

            // Filtro para ignorar movimentos normais (Gravidade ~9.8)
            if (acceleration < 15.0) return

            if (currentTime - lastEventTime > COOLDOWN_MS) {
                // Prioridade ao Acidente (Força maior)
                if (acceleration > ACCIDENT_THRESHOLD) {
                    lastEventTime = currentTime
                    Log.w("FALL_DETECTOR", ">>> ACIDENTE DETETADO! Força: $acceleration")
                    onAccidentDetected?.invoke()
                }
                // Se não for acidente, verifica se é queda
                else if (acceleration > FALL_THRESHOLD) {
                    lastEventTime = currentTime
                    Log.w("FALL_DETECTOR", ">>> QUEDA DETETADA! Força: $acceleration")
                    onFallDetected?.invoke()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }
}