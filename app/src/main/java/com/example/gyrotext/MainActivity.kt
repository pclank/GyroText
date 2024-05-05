package com.example.gyrotext

import android.os.Bundle
import android.text.Editable
import android.text.Selection
import android.text.Selection.extendDown
import android.text.Selection.extendLeft
import android.text.Selection.extendRight
import android.text.Selection.extendUp
import android.text.Selection.moveUp
import android.text.Selection.removeSelection
import android.text.Selection.selectAll
import android.text.Selection.setSelection
import android.text.Spannable
import android.text.SpannableString
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity


enum class GyroInput {
    LEFT, RIGHT, UP, DOWN, FWD, AFT
}

class MainActivity : ComponentActivity() {
    // Many variables
    private var gyroscope: Gyroscope? = null
    lateinit var zeroBut: Button
    private var resetFlag: Boolean = false

    // Text for sensor acceleration values
    lateinit var x_text: TextView
    lateinit var y_text: TextView
    lateinit var z_text: TextView

    // Text for inferred phone orientation values
    lateinit var xPos_text: TextView
    lateinit var yPos_text: TextView
    lateinit var zPos_text: TextView

    // Our selectable text
//    lateinit var test_text: TextView
    lateinit var test_text: EditText

    lateinit var spannable: Spannable

    // Threshold "macros"
    private val thres = 0.2f
    private val xThres = 0.8f
    private val yThres = 0.8f
    private val zThres = 0.9f

    // Zero position of phone
    private var zeroPos: float3 = float3(0.0f, 0.0f, 0.0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        // Initialize all
        zeroBut = findViewById(R.id.zero_but)
        x_text = findViewById(R.id.x_axis_val)
        y_text = findViewById(R.id.y_axis_val)
        z_text = findViewById(R.id.z_axis_val)
        xPos_text = findViewById(R.id.x_pos_val)
        yPos_text = findViewById(R.id.y_pos_val)
        zPos_text = findViewById(R.id.z_pos_val)
        test_text = findViewById(R.id.test_screen_text)

        zeroBut.setOnClickListener { setZeroButton() }

        gyroscope = Gyroscope(this)

        gyroscope!!.setup(this)

        test_text.setOnLongClickListener {
            setZeroButton()

            true
        }

        // listener for gyroscope sensor
        gyroscope!!.setListener(object : Gyroscope.Listener {
            // on rotation method of gyroscope
            override fun onRotation(tx: Float, ty: Float, ts: Float) {
                if (resetFlag)
                    resetZeroPos()

                zeroPos.x += tx
                zeroPos.y += ty
                zeroPos.z += ts

                reportMetrics(tx, ty, ts, zeroPos)

                // Skip if no selection
                if (!test_text.hasSelection())
                    return

                // TODO: Consider better input choice (e.g., using highest value amongst axes)!

                // Y rotation
                if (zeroPos.y > yThres)
                    extendRight(test_text.text, test_text.layout)
                else if (zeroPos.y < -yThres)
                    extendLeft(test_text.text, test_text.layout)

                // X rotation
                // TODO: Stuff here should be delayed a bit by a timer of sorts for better UX!
                if (zeroPos.x > xThres)
                    extendDown(test_text.text, test_text.layout)
                else if (zeroPos.x < -xThres)
                    extendUp(test_text.text, test_text.layout)

                // Z rotation
                if (zeroPos.z > zThres)
                {
                    // TODO: Do many things!
                }
                else if (zeroPos.z < -zThres)
                {
                    // TODO: Do many things!
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        gyroscope?.register()
    }

    override fun onPause() {
        super.onPause()
        gyroscope?.unregister()
    }

    fun reportMetrics(tx: Float, ty: Float, tz: Float, zeroPos: float3)
    {
        x_text.text = "x: "+tx.toString()
        y_text.text = "y: "+ty.toString()
        z_text.text = "z: "+tz.toString()
        xPos_text.text = "xpos: "+zeroPos.x.toString()
        yPos_text.text = "ypos: "+zeroPos.y.toString()
        zPos_text.text = "zpos: "+zeroPos.z.toString()
    }

    fun setZeroButton()
    {
        resetFlag = true
    }

    fun resetZeroPos()
    {
        zeroPos = float3(0.0f, 0.0f, 0.0f)
        resetFlag = false
    }
}
