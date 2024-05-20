package com.example.gyrotext

import android.R.attr.delay
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.os.Handler
import android.text.Selection.extendDown
import android.text.Selection.extendLeft
import android.text.Selection.extendRight
import android.text.Selection.extendToLeftEdge
import android.text.Selection.extendToRightEdge
import android.text.Selection.extendUp
import android.text.Selection.removeSelection
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
    LEFT_MOVE, RIGHT_MOVE, UP_MOVE, DOWN_MOVE, FWD_MOVE, AFT_MOVE, NONE
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

    // Input handling
    private var inputHandler: Handler = Handler()
    private val handlerDelay: Long = 5
    @Volatile
    private var inputList: Array<SensorInput> = arrayOf(SensorInput.NONE, SensorInput.NONE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        // Initialize all
        zeroBut = findViewById(R.id.zero_but)
        devLeftBut = findViewById(R.id.dev_left_but)
        devRightBut = findViewById(R.id.dev_right_but)
        devUpBut = findViewById(R.id.dev_up_but)
        devDownBut = findViewById(R.id.dev_down_but)
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

        // Initialize and set up gyroscope
        gyroscope = Gyroscope(this)
        gyroscope!!.setup(this)

        // Initialize and set up accelerometer
        accelerometer = Accelerometer(this)
        accelerometer!!.setup(this)

        // Initialize timer
        c_timer = CustomTimer(System.currentTimeMillis(), 2000, false, null)

        // Initialize clipboard manager
        clipManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

        // Listeners to zero gyro position on text click
        test_text.setOnLongClickListener {
            setZeroButton()
            inputList = arrayOf(SensorInput.NONE, SensorInput.NONE)             // clear input list
            true
        }

        test_text.setOnClickListener {
            inputList = arrayOf(SensorInput.NONE, SensorInput.NONE)             // clear input list
            setZeroButton()
        }

        // Input handler
        inputHandler.postDelayed(object : Runnable {
            override fun run()
            {
                // TODO: Timer that empties the inputList!

                // Handle inputs
//                if (inputList[0] != null && inputList[1] != null)
                if (inputList[0] != SensorInput.NONE)
                {
                    var accel_index = -1

                    // Check for acceleration input
                    if (inputList[0].ordinal >= 6)
                        accel_index = 0
                    else if (inputList[1] != SensorInput.NONE && inputList[1].ordinal >= 6)
                        accel_index = 1

                    // Give priority to accelerometer input
                    if (accel_index != -1)
                    {
                        updateSelection(inputList[accel_index])
                        inputList = arrayOf(SensorInput.NONE, SensorInput.NONE)
                    }
                    // If acceleration input doesn't exist
                    else
                    {
                        updateSelection(inputList[0])
                        inputList = arrayOf(SensorInput.NONE, SensorInput.NONE)
                    }

                    inputList = arrayOf(SensorInput.NONE, SensorInput.NONE)
                }

                // Set handler
                inputHandler.postDelayed(this, handlerDelay)
            }
        }, handlerDelay)

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
                {
                    if (inputList[0] == SensorInput.NONE)
                    {
                        inputList[0] = SensorInput.RIGHT_ROT
                        return
                    }
                    else if (inputList[1] == SensorInput.NONE && inputList[0] != SensorInput.RIGHT_ROT)
                    {
                        inputList[1] = SensorInput.RIGHT_ROT
                        return
                    }
                }
                else if (zeroRot.y < -gyThres)
                {
                    if (inputList[0] == SensorInput.NONE)
                    {
                        inputList[0] = SensorInput.LEFT_ROT
                        return
                    }
                    else if (inputList[1] == SensorInput.NONE && inputList[0] != SensorInput.LEFT_ROT)
                    {
                        inputList[1] = SensorInput.LEFT_ROT
                        return
                    }
                }

                // X rotation
                if (zeroRot.x > gxThres)
                {
                    if (inputList[0] == SensorInput.NONE)
                    {
                        inputList[0] = SensorInput.DOWN_ROT
                        return
                    }
                    else if (inputList[1] == SensorInput.NONE && inputList[0] != SensorInput.DOWN_ROT)
                    {
                        inputList[1] = SensorInput.DOWN_ROT
                        return
                    }
                }
                else if (zeroRot.x < -gxThres)
                {
                    if (inputList[0] == SensorInput.NONE)
                    {
                        inputList[0] = SensorInput.UP_ROT
                        return
                    }
                    else if (inputList[1] == SensorInput.NONE && inputList[0] != SensorInput.UP_ROT)
                    {
                        inputList[1] = SensorInput.UP_ROT
                        return
                    }
                }

                // Z rotation
                if (zeroRot.z > gzThres)
                {
                    if (inputList[0] == SensorInput.NONE)
                    {
                        inputList[0] = SensorInput.COUNTERCLOCK_ROT
                        return
                    }
                    else if (inputList[1] == SensorInput.NONE && inputList[0] != SensorInput.COUNTERCLOCK_ROT)
                    {
                        inputList[1] = SensorInput.COUNTERCLOCK_ROT
                        return
                    }
                }
                else if (zeroRot.z < -gzThres)
                {
                    if (inputList[0] == SensorInput.NONE)
                    {
                        inputList[0] = SensorInput.CLOCK_ROT
                        return
                    }
                    else if (inputList[1] == SensorInput.NONE && inputList[0] != SensorInput.CLOCK_ROT)
                    {
                        inputList[1] = SensorInput.CLOCK_ROT
                        return
                    }
                }
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

                // Y movement
//                if (ty > ayThres)
//                {
//                    if (inputList[0] == SensorInput.NONE)
//                    {
//                        inputList[0] = SensorInput.FWD_MOVE
//                        return
//                    }
//                    else if (inputList[1] == SensorInput.NONE && inputList[0] != SensorInput.FWD_MOVE)
//                    {
//                        inputList[1] = SensorInput.FWD_MOVE
//                        return
//                    }
//                }
//                else if (ty < -ayThres)
//                {
//                    if (inputList[0] == SensorInput.NONE)
//                    {
//                        inputList[0] = SensorInput.AFT_MOVE
//                        return
//                    }
//                    else if (inputList[1] == SensorInput.NONE && inputList[0] != SensorInput.AFT_MOVE)
//                    {
//                        inputList[1] = SensorInput.AFT_MOVE
//                        return
//                    }
//                }

                // X movement
//                if (tx > axThres)
//                {
//                    if (inputList[0] == SensorInput.NONE)
//                    {
//                        inputList[0] = SensorInput.RIGHT_MOVE
//                        return
//                    }
//                    else if (inputList[1] == SensorInput.NONE && inputList[0] != SensorInput.RIGHT_MOVE)
//                    {
//                        inputList[1] = SensorInput.RIGHT_MOVE
//                        return
//                    }
//                }
//                else if (tx < -axThres)
//                {
//                    if (inputList[0] == SensorInput.NONE)
//                    {
//                        inputList[0] = SensorInput.LEFT_MOVE
//                        return
//                    }
//                    else if (inputList[1] == SensorInput.NONE && inputList[0] != SensorInput.LEFT_MOVE)
//                    {
//                        inputList[1] = SensorInput.LEFT_MOVE
//                        return
//                    }
//                }

                // Z movement
                if (tz > azThres)
                {
                    if (inputList[0] == SensorInput.NONE)
                    {
                        inputList[0] = SensorInput.UP_MOVE
                        return
                    }
                    else if (inputList[1] == SensorInput.NONE && inputList[0] != SensorInput.UP_MOVE)
                    {
                        inputList[1] = SensorInput.UP_MOVE
                        return
                    }
                }
                else if (tz < -azThres)
                {
                    if (inputList[0] == SensorInput.NONE)
                    {
                        inputList[0] = SensorInput.DOWN_MOVE
                        return
                    }
                    else if (inputList[1] == SensorInput.NONE && inputList[0] != SensorInput.DOWN_MOVE)
                    {
                        inputList[1] = SensorInput.DOWN_MOVE
                        return
                    }
                }
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

        if (inputType == SensorInput.FWD_MOVE)
        {
            // TODO: Do many things!
            return
        }
        else if (inputType == SensorInput.AFT_MOVE)
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

            // Set text to clipboard
            val selected_text: CharSequence = test_text.text.subSequence(test_text.selectionStart, test_text.selectionEnd)
            setClipboardClip(selected_text)

            // Remove selection after copy
            removeSelection(test_text.text)

            // Start timer
            c_timer.setTimer(2000, SensorInput.UP_MOVE)

            return
        }
        else if (inputType == SensorInput.DOWN_MOVE)
        {
            // TODO: Do many things!

            return
        }
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
