package com.example.gyrotext

import android.content.Context
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class ExperimentMetrics(
    var userID: Int,
    var expID: Int,
    var startTime: Long,


    )

{
    var duration: Long = 0
    var copyAttempts: Int = 0
    var LEFT_ROT: Int = 0
    var RIGHT_ROT: Int = 0
    var UP_ROT: Int = 0
    var DOWN_ROT: Int = 0
    var CLOCK_ROT: Int = 0
    var COUNTERCLOCK_ROT: Int = 0

    fun saveToJSON(context: Context, fileName: String, externalFileName: String?){
        duration = System.currentTimeMillis() - startTime

        val gson = Gson()
        val jsonString  = gson.toJson(this)

        val file = File("$externalFileName/$fileName")
        try {
            FileOutputStream(file).apply { file.writeText(jsonString) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
