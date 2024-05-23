package com.example.gyrotext

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

data class AccelSensorLog(val timestamp: String)
{
    var x_accel: ArrayList<Float> = arrayListOf()
    var y_accel: ArrayList<Float> = arrayListOf()
    var z_accel: ArrayList<Float> = arrayListOf()
    var list_size = 0

    fun OutputStream.writeCsv()
    {
        val writer = bufferedWriter()
        writer.write(""""X_Accel", "Y_Accel", "Z_Accel"""")
        writer.newLine()

        for (i in 0 until list_size)
        {
            writer.write("${x_accel[i]}, ${y_accel[i]}, ${z_accel[i]}")
            writer.newLine()
        }

        writer.flush()
        writer.close()
    }

    fun WriteLog(absolutePath: String?)
    {
        val file: File = File(absolutePath + "accel_log" + ".csv")
        if (!file.exists())
            file.createNewFile()

        try {
            FileOutputStream(absolutePath+"accel_log.csv").apply { writeCsv() }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
