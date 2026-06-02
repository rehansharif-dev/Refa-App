package com.example.transportapp.presentation.util

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(private val onShake: (count: Int) -> Unit) : SensorEventListener {

    private var lastShakeTime: Long = 0
    private var shakeCount = 0
    private val SHAKE_THRESHOLD = 12.0f
    private val SHAKE_INTERVAL = 500L // ms

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
        if (acceleration > SHAKE_THRESHOLD) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTime > SHAKE_INTERVAL) {
                if (now - lastShakeTime > 1500L) {
                    shakeCount = 1
                } else {
                    shakeCount++
                }
                lastShakeTime = now
                onShake(shakeCount)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}