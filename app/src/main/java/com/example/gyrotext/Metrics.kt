package com.example.gyrotext

import android.content.Context
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class Metrics(
    // TODO: Get the user ID as a variable for the data class.
    var gxmax: Float,
    var gymax: Float,
    var gzmax: Float,
    var tiltxmax: Float,
    var tiltymax: Float,
    var tiltzmax: Float,
    var maxFWD: Float,
    var maxBWD: Float
){
    constructor(value: Float) : this(value, value, value, value, value, value, value, value)
    fun saveToJSON(context: Context, fileName: String, externalFileName: String?){
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
