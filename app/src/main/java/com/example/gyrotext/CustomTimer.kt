package com.example.gyrotext

class CustomTimer {
    var startTime: Long = 0
    var delay: Int = 0
    var enabled: Boolean = false

    fun setTimer(dl: Int)
    {
        delay = dl
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