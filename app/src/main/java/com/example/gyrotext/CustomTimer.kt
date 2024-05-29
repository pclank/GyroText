package com.example.gyrotext

class CustomTimer(var startTime: Long, var delay: Int, var enabled: Boolean, var inputType: SensorInput?) {
//    var startTime: Long = 0             // in milliseconds
//    var delay: Int = 0
//    var enabled: Boolean = false
//    val inputType: SensorInput? = null

    fun setTimer(dl: Int, iType: SensorInput)
    {
        delay = dl
        inputType = iType
        startTime = System.currentTimeMillis()
        enabled = true
    }

    fun checkTimer(): Boolean
    {
        if (!enabled || System.currentTimeMillis() - startTime > delay)
        {
            enabled = false
            return true
        }

        return false
    }
}