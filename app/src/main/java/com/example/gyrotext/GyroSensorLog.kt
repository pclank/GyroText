package com.example.gyrotext

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream


data class GyroSensorLog(val timestamp: String)
{
    var x_gyro: ArrayList<Float> = arrayListOf()
    var y_gyro: ArrayList<Float> = arrayListOf()
    var z_gyro: ArrayList<Float> = arrayListOf()
    var list_size = 0

    fun OutputStream.writeCsv()
    {
        val writer = bufferedWriter()
        writer.write(""""X_Gyro", "Y_Gyro", "Z_Gyro"""")
        writer.newLine()

        for (i in 0 until list_size)
        {
            writer.write("${x_gyro[i]}, ${y_gyro[i]}, ${z_gyro[i]}")
            writer.newLine()
        }

        writer.flush()
        writer.close()
    }

    fun WriteLog(absolutePath: String?)
    {
        val file: File = File(absolutePath + "gyro_log" + ".csv")
        if (!file.exists())
            file.createNewFile()

        try {
            FileOutputStream(absolutePath+"gyro_log.csv").apply { writeCsv() }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
