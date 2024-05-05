package com.example.gyrotext

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.gyrotext.ui.theme.GyroTextTheme

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

    // Threshold "macros"
    private val thres = 0.2f
    private val xThres = 0.8f
    private val yThres = 0.8f

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

        zeroBut.setOnClickListener { setZeroButton() }

        gyroscope = Gyroscope(this)

        gyroscope!!.setup(this)

        // create a listener for gyroscope
        gyroscope!!.setListener(object : Gyroscope.Listener {
            // on rotation method of gyroscope
            override fun onRotation(tx: Float, ty: Float, tz: Float) {
                if (resetFlag)
                    resetZeroPos()

                zeroPos.x += tx
                zeroPos.y += ty
                zeroPos.z += tz

                // TODO: Consider better input choice (e.g., using highest value)!

//                if (zeroPos.y > yThres)
//                    cView.updateAABB(100)
//                else if (zeroPos.y < -yThres)
//                    cView.updateAABB(-100)
//
//                if (zeroPos.x > xThres)
//                    cView.addAABB()
//                else if (zeroPos.x < -xThres)
//                    cView.deleteAABB()

                reportMetrics(tx, ty, tz, zeroPos)
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
