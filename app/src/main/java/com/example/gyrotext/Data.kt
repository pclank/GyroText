package com.example.gyrotext

import android.content.Context
import com.google.gson.Gson
import java.io.File

data class Metrics(

    // TODO: Get the user ID as a variable fo the data class.
    var gxmax: Float,
    var gymax: Float,
    var gzmax: Float,
    var tiltxmax: Float,
    var tiltymax: Float,
    var tiltzmax: Float,
    var maxFWD: Float,
    var maxBWD: Float
){
    fun saveToJSON(context: Context, fileName: String){

        val gson = Gson()
        val jsonString  = gson.toJson(this)

        val file = File(context.filesDir, fileName)
        file.writeText(jsonString)
    }
}
