package com.example.gyrotext

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.text.Selection.extendDown
import android.text.Selection.extendLeft
import android.text.Selection.extendRight
import android.text.Selection.extendToLeftEdge
import android.text.Selection.extendToRightEdge
import android.text.Selection.extendUp
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import kotlin.math.abs
import kotlin.math.max



// Fake Macros, because Kotlin doesn't have them... sad
val ACCEL_ENABLED: Boolean = true              // whether the linear accelerometer functions are enabled

// Enum that contains all inputs for all of our sensors (i.e., gyroscope and accelerometer)
enum class SensorInput {
    LEFT_ROT, RIGHT_ROT, UP_ROT, DOWN_ROT, CLOCK_ROT, COUNTERCLOCK_ROT,
    LEFT_MOVE, RIGHT_MOVE, UP_MOVE, DOWN_MOVE, FWD, AFT
}

class MainActivity : ComponentActivity() {
    // Many variables
    private var gyroscope: Gyroscope? = null
    private var accelerometer: Accelerometer? = null
    private lateinit var c_timer: CustomTimer
    private lateinit var clipManager: ClipboardManager
    lateinit var zeroBut: Button
    private var resetFlag: Boolean = false

    // Development buttons
    // TODO: Remove or hide in release version
    lateinit var devLeftBut: Button
    lateinit var devRightBut: Button
    lateinit var devUpBut: Button
    lateinit var devDownBut: Button
    lateinit var testMetricsBut: Button

    // Development metrics values
    private var g_max_x = 0f
    private var g_max_y = 0f
    private var g_max_z = 0f
    private var maxtx = 0f
    private var maxty = 0f
    private var maxtz = 0f
    private var maxFwd = 0f
    private var maxBwd = 0f

    // Text for sensor acceleration values
    lateinit var g_maxx_text: TextView
    lateinit var g_maxy_text: TextView
    lateinit var g_maxz_text: TextView
    lateinit var a_x_text: TextView
    lateinit var a_y_text: TextView
    lateinit var a_z_text: TextView
    lateinit var a_mFwd_text: TextView
    lateinit var a_mBwd_text: TextView

    //Text for tilt values
    lateinit var maxtiltx_text: TextView
    lateinit var maxtilty_text: TextView
    lateinit var maxtiltz_text: TextView

    // Text for inferred phone orientation values
    lateinit var xPos_text: TextView
    lateinit var yPos_text: TextView
    lateinit var zPos_text: TextView

    // Our selectable text
    lateinit var test_text: EditText

    // Threshold "macros" for Gyro
    private val gxThres = 0.8f
    private val gyThres = 0.8f
    private val gzThres = 0.9f

    // Threshold "macros" for Accel
//    private val axThres = 1.5f
//    private val ayThres = 1.5f
//    private val azThres = 1.5f
    private val axThres = 3.2f
    private val ayThres = 3.2f
    private val azThres = 3.2f

    // Reps for left-right extension functions (kotlin hates unsigned ints???)
    private val leftRep: Int = 1
    private val rightRep: Int = 1

    // Zero position of phone rotation
    private var zeroRot: float3 = float3(0.0f, 0.0f, 0.0f)
    
    // Zero position of phone position (great sentence)
    private var zeroPos: float3 = float3(0.0f, 0.0f, 0.0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        // Initialize all
        zeroBut = findViewById(R.id.zero_but)
        devLeftBut = findViewById(R.id.dev_left_but)
        devRightBut = findViewById(R.id.dev_right_but)
        devUpBut = findViewById(R.id.dev_up_but)
        devDownBut = findViewById(R.id.dev_down_but)
        testMetricsBut = findViewById(R.id.testing_but)
        g_maxx_text = findViewById(R.id.x_axis_val)
        g_maxy_text = findViewById(R.id.y_axis_val)
        g_maxz_text = findViewById(R.id.z_axis_val)
        a_x_text = findViewById(R.id.a_x_axis_val)
        a_y_text = findViewById(R.id.a_y_axis_val)
        a_z_text = findViewById(R.id.a_z_axis_val)
        xPos_text = findViewById(R.id.x_pos_val)
        yPos_text = findViewById(R.id.y_pos_val)
        zPos_text = findViewById(R.id.z_pos_val)
        test_text = findViewById(R.id.test_screen_text)
        maxtiltx_text = findViewById(R.id.maxtiltx_val)
        maxtilty_text = findViewById(R.id.maxtilty_val)
        maxtiltz_text = findViewById(R.id.maxtiltz_val)
        a_mFwd_text = findViewById(R.id.a_maxfwd_val)
        a_mBwd_text = findViewById(R.id.a_maxbwd_val)

        // Button listeners
        zeroBut.setOnClickListener { setZeroButton() }
        devLeftBut.setOnClickListener { updateSelection(SensorInput.LEFT_ROT) }
        devRightBut.setOnClickListener { updateSelection(SensorInput.RIGHT_ROT) }
        devUpBut.setOnClickListener { updateSelection(SensorInput.UP_ROT) }
        devDownBut.setOnClickListener { updateSelection(SensorInput.DOWN_ROT) }
        testMetricsBut.setOnClickListener {
            saveMetrics(g_max_x, g_max_y, g_max_z, maxtx, maxty, maxtz, maxFwd, maxBwd)
        }

        // Initialize and set up gyroscope
        gyroscope = Gyroscope(this)
        gyroscope!!.setup(this)

        // Initialize and set up accelerometer
        accelerometer = Accelerometer(this)
        accelerometer!!.setup(this)

        // Initialize timer
        c_timer = CustomTimer()

        // Initialize clipboard manager
        clipManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

        // Listeners to zero gyro position on text click
        test_text.setOnLongClickListener {
            setZeroButton()
            true
        }

        test_text.setOnClickListener {
            setZeroButton()
        }

        // listener for gyroscope sensor (non-null assertion)
        gyroscope!!.setListener(object : Gyroscope.Listener {
            // on rotation method of gyroscope
            override fun onRotation(tx: Float, ty: Float, tz: Float) {
                if (resetFlag)
                    resetZeroRot()

                zeroRot.x += tx
                zeroRot.y += ty
                zeroRot.z += tz

                //Maximum tilt values
                maxtx = max(maxtx, abs(zeroRot.x))
                maxty = max(maxty, abs(zeroRot.y))
                maxtz = max(maxtz, abs(zeroRot.z))

                //Maximum angular acceleration values
                g_max_x = max(g_max_x, abs(tx))
                g_max_y = max(g_max_y, abs(ty))
                g_max_z = max(g_max_z, abs(tz))

                reportGyroMetrics(g_max_x, g_max_y, g_max_z, maxtx, maxty, maxtz, zeroRot)

                // Skip if no selection
                if (!test_text.hasSelection())
                    return

                // TODO: Consider better input choice (e.g., using highest value amongst axes)!

                // Y rotation
                if (zeroRot.y > gyThres)
                    updateSelection(SensorInput.RIGHT_ROT)
                else if (zeroRot.y < -gyThres)
                    updateSelection(SensorInput.LEFT_ROT)

                // X rotation
                // TODO: Stuff here should be delayed a bit by a timer of sorts for better UX!
                if (zeroRot.x > gxThres)
                    updateSelection(SensorInput.DOWN_ROT)
                else if (zeroRot.x < -gxThres)
                    updateSelection(SensorInput.UP_ROT)

                // Z rotation
                if (zeroRot.z > gzThres)
                    updateSelection(SensorInput.COUNTERCLOCK_ROT)
                else if (zeroRot.z < -gzThres)
                    updateSelection(SensorInput.CLOCK_ROT)
            }
        })

        // listener for accelerometer sensor (non-null assertion)
        accelerometer!!.setListener(object : Accelerometer.Listener {
            // on movement method
            override fun onMovement(tx: Float, ty: Float, tz: Float) {
                if (resetFlag)
                    resetZeroPos()

                zeroPos.x += tx
                zeroPos.y += ty
                zeroPos.z += tz

                if (tz > 0 && (abs(tz) > maxFwd)) {
                    maxFwd = abs(tz)
                }
                else if (tz < 0 && (abs(tz) > maxBwd)) {
                    maxBwd = abs(tz)
                }

                reportAccelMetrics(maxFwd, maxBwd, zeroPos)

                // Skip if no selection
                if (!test_text.hasSelection())
                    return

                // TODO: Consider better input choice (e.g., using highest value amongst axes)!
                // TODO: Stuff here should be delayed a bit by a timer of sorts for better UX!

//                // Y movement
//                if (zeroPos.y > ayThres)
//                    updateSelection(SensorInput.FWD)
//                else if (zeroPos.y < -ayThres)
//                    updateSelection(SensorInput.AFT)
//
//                // X movement
//                if (zeroPos.x > axThres)
//                    updateSelection(SensorInput.RIGHT_MOVE)
//                else if (zeroPos.x < -axThres)
//                    updateSelection(SensorInput.LEFT_MOVE)
//
//                // Z movement
//                if (zeroPos.z > azThres)
//                    updateSelection(SensorInput.UP_MOVE)
//                else if (zeroPos.z < -azThres)
//                    updateSelection(SensorInput.DOWN_MOVE)

                // Y movement
                if (ty > ayThres)
                    updateSelection(SensorInput.FWD)
                else if (ty < -ayThres)
                    updateSelection(SensorInput.AFT)

                // X movement
                if (tx > axThres)
                    updateSelection(SensorInput.RIGHT_MOVE)
                else if (tx < -axThres)
                    updateSelection(SensorInput.LEFT_MOVE)

                // Z movement
                if (tz > azThres)
                    updateSelection(SensorInput.UP_MOVE)
                else if (tz < -azThres)
                    updateSelection(SensorInput.DOWN_MOVE)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        gyroscope?.register()
        accelerometer?.register()
    }

    override fun onPause() {
        super.onPause()
        gyroscope?.unregister()
        accelerometer?.unregister()
    }

    fun reportGyroMetrics(gmaxtx: Float, gmaxty: Float, gmaxtz : Float, maxtiltx : Float, maxtilty : Float, maxtiltz : Float, zeroRot: float3)
    {
        //Update and show maximum values
        //Angular Acceleration

        g_maxx_text.text = String.format("%.2f", gmaxtx)
        g_maxy_text.text = String.format("%.2f", gmaxty)
        g_maxz_text.text = String.format("%.2f", gmaxtz)

        //Tilt
        maxtiltx_text.text = String.format("%.2f", maxtiltx)
        maxtilty_text.text = String.format("%.2f", maxtilty)
        maxtiltz_text.text = String.format("%.2f", maxtiltz)

        // TODO: Figure out if these are still used. If not delete them from here.
        xPos_text.text = String.format("%.2f", zeroRot.x)
        yPos_text.text = String.format("%.2f", zeroRot.y)
        zPos_text.text = String.format("%.2f", zeroRot.z)


    }

    fun reportAccelMetrics(mFwd: Float, mBwd: Float, zeroPos: float3)
    {
        //a_x_text.text = "ax: "+atx.toString()
        //a_y_text.text = "ay: "+aty.toString()
        //a_z_text.text = "az: "+atz.toString()
        a_mFwd_text.text = String.format("%.2f", mFwd)
        a_mBwd_text.text = String.format("%.2f", mBwd)

        // TODO: Add position to layout!
    }
    
    fun updateSelection(inputType: SensorInput)
    {
        require(rightRep > 0 && leftRep > 0) { R.string.left_right_assertion_error }

        // Gyro stuff
        if (inputType == SensorInput.RIGHT_ROT)
        {
            for (i in 0 until rightRep)
                extendRight(test_text.text, test_text.layout)

            return
        }
        else if (inputType == SensorInput.LEFT_ROT)
        {
            for (i in 0 until leftRep)
                extendLeft(test_text.text, test_text.layout)

            return
        }

        if (inputType == SensorInput.DOWN_ROT)
        {
            extendDown(test_text.text, test_text.layout)
            return
        }
        else if (inputType == SensorInput.UP_ROT)
        {
            extendUp(test_text.text, test_text.layout)
            return
        }

        if (inputType == SensorInput.CLOCK_ROT)
        {
            extendToRightEdge(test_text.text, test_text.layout)
            return
        }

        else if (inputType == SensorInput.COUNTERCLOCK_ROT)
        {
            extendToLeftEdge(test_text.text, test_text.layout)
            return
        }

        // Linear accelerometer stuff
        // TODO: Remove after working version!
        if (!ACCEL_ENABLED)
            return

        if (inputType == SensorInput.FWD)
        {
            // TODO: Do many things!
            return
        }
        else if (inputType == SensorInput.AFT)
        {
            // TODO: Do many things!
            return
        }

//        if (inputType == SensorInput.RIGHT_MOVE)
//        {
//            if (!c_timer.checkTimer())
//                return
//
//            extendToRightEdge(test_text.text, test_text.layout)
//            c_timer.setTimer(2000)
//        }
//        else if (inputType == SensorInput.LEFT_MOVE)
//        {
//            if (!c_timer.checkTimer())
//                return
//
//            extendToLeftEdge(test_text.text, test_text.layout)
//            c_timer.setTimer(2000)
//        }

        if (inputType == SensorInput.UP_MOVE)
        {
            if (!c_timer.checkTimer())
                return

            val selected_text: CharSequence = test_text.text.subSequence(test_text.selectionStart, test_text.selectionEnd)
            setClipboardClip(selected_text)
            c_timer.setTimer(2000)

            return
        }
        else if (inputType == SensorInput.DOWN_MOVE)
        {
            // TODO: Do many things!

            return
        }
    }

    private fun saveMetrics(gxmax: Float, gymax: Float, gzmax: Float, tiltxmax: Float, tiltymax: Float, tiltzmax: Float, maxFWD: Float, maxBWD: Float){
        val metrics = Metrics(gxmax, gymax, gzmax, tiltxmax, tiltymax, tiltzmax, maxFWD, maxBWD)
        metrics.saveToJSON(this, "metrics.json")
    }

    private fun setZeroButton()
    {
        resetFlag = true
    }

    fun resetZeroRot()
    {
        zeroRot = float3(0.0f, 0.0f, 0.0f)
        resetFlag = false
    }

    fun resetZeroPos()
    {
        zeroPos = float3(0.0f, 0.0f, 0.0f)
        resetFlag = false
    }

    private fun setClipboardClip(new_text: CharSequence)
    {
        // Copy to clipboard and set as primary clip
        val new_clip = ClipData.newPlainText("label", new_text)
        clipManager.setPrimaryClip(new_clip)

        // Message user
        Toast.makeText(
            this, R.string.copy_action_msg,
            Toast.LENGTH_LONG
        ).show()
    }
}
