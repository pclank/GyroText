package com.example.gyrotext

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class Accelerometer(context: Context) {
    interface Listener {
        fun onMovement(tx: Float, ty: Float, tz: Float)
    }

    // create an instance
    private var listener: Listener? = null

    // method to set the instance
    fun setListener(l: Listener?) {
        listener = l
    }

    private var sensorManager: SensorManager? = null
    private var sensor: Sensor? = null
    private var sensorEventListener: SensorEventListener? = null

    // create constructor with context as argument
    fun setup(context: Context)
    {

        // create instance of sensor manager
        // TODO: Could potentially also do with a single instance, but performance gains unlikely
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // create instance of sensor with type accelerometer
//        sensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)      // this gets rid of gravity, which is nice

        // create the sensor listener
        sensorEventListener = object : SensorEventListener {
            // call when movement was detected
            override fun onSensorChanged(sensorEvent: SensorEvent) {
                if (listener != null)
                {
                    listener!!.onMovement(
                        sensorEvent.values[0], sensorEvent.values[1],
                        sensorEvent.values[2]
                    )
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, i: Int) {}
        }
    }

    // create register method for notifs
    fun register() {
        sensorManager!!.registerListener(
            sensorEventListener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    // create method to unregister from notifs
    fun unregister() {
        sensorManager!!.unregisterListener(sensorEventListener)
    }
}