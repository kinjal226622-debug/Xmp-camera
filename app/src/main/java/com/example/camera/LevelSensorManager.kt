package com.example.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class LevelAngles(
    val rollDegrees: Float = 0f,
    val pitchDegrees: Float = 0f,
    val isPerfectLevel: Boolean = false
)

class LevelSensorManager(context: Context, private val onAnglesChanged: (LevelAngles) -> Unit) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val gravity = FloatArray(3)
    private var hasGravity = false

    fun start() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        // Low-pass filter to smooth sensor readings
        val alpha = 0.8f
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]
        hasGravity = true

        val ax = gravity[0]
        val ay = gravity[1]
        val az = gravity[2]

        val roll = Math.toDegrees(atan2(-ax.toDouble(), ay.toDouble())).toFloat()
        val pitch = Math.toDegrees(atan2(az.toDouble(), sqrt((ax * ax + ay * ay).toDouble()))).toFloat()

        // Normalize roll towards 0 (level)
        var normalizedRoll = roll
        while (normalizedRoll > 90f) normalizedRoll -= 180f
        while (normalizedRoll < -90f) normalizedRoll += 180f

        val isLevel = kotlin.math.abs(normalizedRoll) < 1.0f

        onAnglesChanged(
            LevelAngles(
                rollDegrees = (normalizedRoll * 10f).roundToInt() / 10f,
                pitchDegrees = (pitch * 10f).roundToInt() / 10f,
                isPerfectLevel = isLevel
            )
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
