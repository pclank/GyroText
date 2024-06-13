package com.example.gyrotext

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.InputType
import android.text.Selection.extendDown
import android.text.Selection.extendLeft
import android.text.Selection.extendRight
import android.text.Selection.extendToLeftEdge
import android.text.Selection.extendToRightEdge
import android.text.Selection.extendUp
import android.text.Selection.removeSelection
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import kotlin.math.abs
import kotlin.math.max


// Fake Macros, because Kotlin doesn't have them... sad
val ACCEL_ENABLED: Boolean = true              // whether the linear accelerometer functions are enabled

// Enum that contains all inputs for all of our sensors (i.e., gyroscope and accelerometer)
enum class SensorInput {
    LEFT_ROT, RIGHT_ROT, UP_ROT, DOWN_ROT, CLOCK_ROT, COUNTERCLOCK_ROT,
    LEFT_MOVE, RIGHT_MOVE, UP_MOVE, DOWN_MOVE, FWD_MOVE, AFT_MOVE, NONE
}

enum class ExperimentType {
    GYROTEXT, DEFAULT
}

class MainActivity : ComponentActivity() {
    // Many variables
    private var gyroscope: Gyroscope? = null
    private var accelerometer: Accelerometer? = null
    private lateinit var c_timer: CustomTimer
    private lateinit var updown_timer: CustomTimer
    private lateinit var clipManager: ClipboardManager
    private var resetFlag: Boolean = false

    // Vibrator
    private lateinit var vibrator: Vibrator

    // Development buttons
    // TODO: Remove or hide in release version
    lateinit var testMetricsBut: Button
    lateinit var copyBut: Button
    lateinit var idBut: Button
    lateinit var startPracticeBut: Button

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
    private val gxThres = 6.0f
    private val gyThres = 5.0f
    private val gzThres = 5.0f

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
    private val handlerDelay: Long = 10
    @Volatile
    private var inputList: Array<SensorInput> = arrayOf(SensorInput.NONE, SensorInput.NONE)

    // Input field
    private lateinit var idInput: EditText

    // Experiment stuff
    private lateinit var experimentList: MutableList<ExperimentPage>
    private var current_exp_id = 0
    private lateinit var current_exp_metrics: ExperimentMetrics
    private var userID: Int = -1                            // It's obvious
    private var practiceFlag: Boolean = true
    private lateinit var methodText: TextView

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

//        Thread.setDefaultUncaughtExceptionHandler { paramThread, paramThrowable -> //Catch your exception
//            // Without System.exit() this will not work.
//            System.exit(2)
//        }

        // Initialize all
        testMetricsBut = findViewById(R.id.test_but)
        copyBut = findViewById(R.id.copy_but)
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
        idInput = findViewById(R.id.user_id_text)
        idBut = findViewById(R.id.id_input_but)
        startPracticeBut = findViewById(R.id.start_but)
        methodText = findViewById(R.id.method_text)

        // Experiment objects
        experimentList = mutableListOf()
        val exp0 = ExperimentPage("Wrote water woman of heart it total other. By in entirely securing suitable graceful at families improved. Zealously few furniture repulsive was agreeable consisted difficult. Collected breakfast noise off estimable questions in to favourite it. Known he place worth words it as to. Spoke now smart her ready. Built purse maids cease her ham new seven among and. Pulled coming wooded tended it answer remain me be. So landlord by we unlocked sensible it. Fat cannot use denied excuse son law. Wisdom happen suffer common the appear ham beauty her had. Or belonging zealously existence as by resources.", "Known he place worth words it as to.", 0, ExperimentType.GYROTEXT)
        val exp1 = ExperimentPage("Sigh view am high neat half to what. Sent late held than set why wife our. If an blessing building steepest. Agreement distrusts mrs six affection satisfied. Day blushes visitor end company discretion prevent chapter. Consider declared out expenses her concerns. No at indulgence conviction particular unsatiable all ignorant no supplied boisterous old. Direct enough off others say eldest may get. You vexed shy mirth now noise. Possible Exeter settling marriage recurred. Talked him people valley add use her depend letter. Allowance too applauded now way something recommend.", "Direct enough off others say eldest may get.", 1, ExperimentType.DEFAULT)
        val exp2 = ExperimentPage("Difficulty on insensible reasonable in. From as went he they. Preference themselves me as thoroughly partiality considered on in estimating. Middletons acceptance discovered projecting so is so or. In or attachment L88423PX(Aedean) inquietude remarkably comparison at an. Is surrounded prosperous stimulated am me discretion expression. But truth being state can she china widow. Occasional preference fat remarkably now projecting uncommonly dissimilar. Sentiments projection particular companions interested do at my delightful. Listening newspaper in advantage frankness to concluded unwilling.", "L88423PX", 2, ExperimentType.GYROTEXT)
        val exp3 = ExperimentPage("Throwing consider dwelling bachelor joy her proposal laughter. Raptures returned disposed one entirely her men ham. By to admire vanity county an mutual as roused. Of an thrown am warmly merely result depart supply. Required honoured X-89758FR(Tranvavia) trifling eat pleasure man relation. Assurance yet bed was improving furniture man. Distrusts delighted she listening mrs extensive admitting far. Written enquire painful ye to offices forming it. Then so does over sent dull on. Likewise offended humoured mrs fat trifling answered. On ye position greatest so desirous.", "X-89758FR", 3, ExperimentType.DEFAULT)
        val exp4 = ExperimentPage("Savings her pleased are several started females met. Short her not among being any. Thing of judge fruit charm views do. Miles mr an forty along as he. She education get middleton day agreement performed preserved unwilling. Do however as pleased offence outward beloved by present. By outward neither he so covered amiable greater. Juvenile proposal betrayed he an informed weddings followed. Precaution day see imprudence sympathize principles. At full leaf give quit to in they up. Is branched in my up strictly remember. Songs but chief has ham widow downs.", "Savings her pleased are several started females met.", 4, ExperimentType.GYROTEXT)
        val exp5 = ExperimentPage("Unfeeling so rapturous discovery or he reasonably so. Middletons exquisite impression by terminated. Old pleasure required removing elegance him had. Down she bore sing saw calm high. Of an or game gate west face shed. no great but music too old found arose. Seen you eyes son show. Far two unaffected one alteration apartments celebrated but middletons interested. Described deficient applauded consisted my me do. Passed edward two talent effect seemed engage six. On ye great do child sorry lived. Proceed cottage far letters ashamed get clothes day. Stairs regret at if matter to.", "Unfeeling so rapturous discovery or he reasonably so.", 5, ExperimentType.DEFAULT)
        val exp6 = ExperimentPage("Expenses as material breeding insisted building to in. Continual so distrusts pronounce by unwilling listening. Thing do taste on we manor. Him had wound use found hoped. Of distrusts immediate enjoyment curiosity do. Marianne numerous saw thoughts the humored. Ye in mr evil. Spoke as as other again ye. Hard on to roof he drew so incommode sell side. Longer waited mr of nature seemed. Improving knowledge objection me ye is prevailed principle in. Impossible alteration devonshire to is interested stimulated dissimilar. To matter esteem polite do if.", "Hard on to roof he drew so incommode sell side.", 6, ExperimentType.GYROTEXT)
        val exp7 = ExperimentPage("Its sometimes her behaviour are contented. Do listening am eagerness oh objection collected. Together gay feelings continue juvenile had off one. Unknown may service subject her letters one bed. Child years noise ye in forty. Loud in this in both hold. My travelling entrance me is disposal bachelor remember relation. New the her nor case that lady paid read. Invitation friendship eat everything the out two. Shy you who scarcely debating resolved. Always polite on is warmth spirit it to hearts. Downs those still witty an balls so chief so. Moment an little remain no up lively no.", "New the her nor case that lady paid read.", 7, ExperimentType.DEFAULT)
        val exp8 = ExperimentPage("Is at purse tried jokes china ready decay an. Small its shy way had woody downs power. To denoting admitted speaking learning my exercise so in. Procured shutters mr it feelings. To or three offer house begin taken am at. As dissuade cheerful overcame so of friendly he indulged unpacked. Alteration connection to so as collecting me. Difficult in delivered extensive at direction allowance. Alteration put use diminution can considered sentiments interested discretion. An seeing feebly stairs am branch income me unable. Suppose end get boy warrant general natural.", "cheerful overcame so of friendly he indulged unpacked. Alteration con", 8, ExperimentType.GYROTEXT)
        val exp9 = ExperimentPage("Ask especially collecting terminated may son expression. Extremely eagerness principle estimable own was man. Men received far his dashwood subjects new. My sufficient surrounded an companions dispatched in on. Connection too unaffected expression led son possession. New smiling friends and outweigh her another. Leaf she does none love high yet. Snug love will up bore as be. Pursuit man son musical general pointed. It surprise informed mr advanced do. Yet bed any for travelling assistance indulgence unpleasing. Not thoughts all exercise blessing. Indulgence way everything.", "unaffected expression led son possession. New smiling friends and out", 9, ExperimentType.DEFAULT)

        experimentList.add(exp0)
        experimentList.add(exp1)
        experimentList.add(exp2)
        experimentList.add(exp3)
        experimentList.add(exp4)
        experimentList.add(exp5)
        experimentList.add(exp6)
        experimentList.add(exp7)
        experimentList.add(exp8)
        experimentList.add(exp9)

        current_exp_id = 0
//        setupExperiment(current_exp_id)

        // Button listeners
        testMetricsBut.setOnClickListener {
            saveMetrics(g_max_x, g_max_y, g_max_z, maxtx, maxty, maxtz, maxFwd, maxBwd)
        }
        copyBut.setOnClickListener {
            // Flip selection indices if needed
            var sel_start = test_text.selectionStart
            var sel_end = test_text.selectionEnd
            if (test_text.selectionStart > test_text.selectionEnd)
            {
                sel_start = test_text.selectionEnd
                sel_end = test_text.selectionStart
            }

            // Set text to clipboard
            val selected_text: CharSequence = test_text.text.subSequence(sel_start, sel_end)
            setClipboardClip(selected_text)

            // Remove selection after copy
            removeSelection(test_text.text)
        }

        idBut.setOnClickListener {
            startPractice()
        }

        startPracticeBut.setOnClickListener {
            startExperiment()
        }

        // Starting screen setup
        methodText.visibility = View.GONE
        test_text.visibility = View.GONE
        startPracticeBut.visibility = View.GONE
        copyBut.visibility = View.GONE

        // Initialize and set up gyroscope
        gyroscope = Gyroscope(this)
        gyroscope!!.setup(this)

        // Initialize and set up accelerometer
        accelerometer = Accelerometer(this)
        accelerometer!!.setup(this)

        // Initialize timer
        c_timer = CustomTimer(System.currentTimeMillis(), 2000, false, null)
        updown_timer = CustomTimer(System.currentTimeMillis(), 2000, false, null)

        vibrator = getSystemService(Vibrator::class.java)

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
                        if (inputList[0] == SensorInput.UP_ROT || inputList[0] == SensorInput.DOWN_ROT){
                            if (!updown_timer.checkTimer()){
                                inputList = arrayOf(SensorInput.NONE, SensorInput.NONE)
                                return
                            }
                            else{
                                updown_timer.setTimer(1000, SensorInput.NONE)
                            }
                        }
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
                if (!practiceFlag && experimentList[current_exp_id].expType == ExperimentType.DEFAULT)
                    return

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
                if (!practiceFlag && experimentList[current_exp_id].expType == ExperimentType.DEFAULT)
                    return

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
    
    @RequiresApi(Build.VERSION_CODES.Q)
    fun updateSelection(inputType: SensorInput)
    {
        require(rightRep > 0 && leftRep > 0) { R.string.left_right_assertion_error }

        if (inputType == SensorInput.NONE)
            return

        // Gyro stuff
        if (inputType == SensorInput.RIGHT_ROT)
        {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))

            for (i in 0 until rightRep)
                extendRight(test_text.text, test_text.layout)

            if (!practiceFlag)
                current_exp_metrics.RIGHT_ROT++
            return
        }
        else if (inputType == SensorInput.LEFT_ROT)
        {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))

            for (i in 0 until leftRep)
                extendLeft(test_text.text, test_text.layout)

            if (!practiceFlag)
                current_exp_metrics.LEFT_ROT++
            return
        }

        if (inputType == SensorInput.DOWN_ROT)
        {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))

            extendDown(test_text.text, test_text.layout)

            if (!practiceFlag)
                current_exp_metrics.DOWN_ROT++
            return
        }
        else if (inputType == SensorInput.UP_ROT)
        {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))

            extendUp(test_text.text, test_text.layout)

            if (!practiceFlag)
                current_exp_metrics.UP_ROT++
            return
        }

        if (inputType == SensorInput.CLOCK_ROT)
        {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))

            extendToRightEdge(test_text.text, test_text.layout)

            if (!practiceFlag)
                current_exp_metrics.CLOCK_ROT++
            return
        }

        else if (inputType == SensorInput.COUNTERCLOCK_ROT)
        {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))

            extendToLeftEdge(test_text.text, test_text.layout)

            if (!practiceFlag)
                current_exp_metrics.COUNTERCLOCK_ROT++
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

            // Flip selection indices if needed
            var sel_start = test_text.selectionStart
            var sel_end = test_text.selectionEnd
            if (test_text.selectionStart > test_text.selectionEnd)
            {
                sel_start = test_text.selectionEnd
                sel_end = test_text.selectionStart
            }

            // Set text to clipboard
            val selected_text: CharSequence = test_text.text.subSequence(sel_start, sel_end)
            setClipboardClip(selected_text)

            // Remove selection after copy
            removeSelection(test_text.text)

            // Start timer
            c_timer.setTimer(2000, SensorInput.UP_MOVE)

            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))

            return
        }
        else if (inputType == SensorInput.DOWN_MOVE)
        {
            // TODO: Do many things!

            return
        }
    }

    private fun saveMetrics(gxmax: Float, gymax: Float, gzmax: Float, tiltxmax: Float, tiltymax: Float, tiltzmax: Float, maxFWD: Float, maxBWD: Float){
        val externalStorageDir = getExternalFilesDir(Environment.getDataDirectory().absolutePath)?.absolutePath

        val metrics = Metrics(gxmax, gymax, gzmax, tiltxmax, tiltymax, tiltzmax, maxFWD, maxBWD)
        metrics.saveToJSON(this, "metrics.json", externalStorageDir)
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
        try {
            if (!practiceFlag)
                current_exp_metrics.copyAttempts++
            // Copy to clipboard and set as primary clip
            val new_clip = ClipData.newPlainText("label", new_text)
            clipManager.setPrimaryClip(new_clip)

            // Compare copied text
            if (!practiceFlag && experimentList[current_exp_id].tgtText == new_text.toString())
                switchExperiment()

            // Message user
            Toast.makeText(
                this, R.string.copy_action_msg,
                Toast.LENGTH_LONG
            ).show()
        } catch (e: StringIndexOutOfBoundsException) {
            // handler
            Toast.makeText(
                this, R.string.copy_action_msg,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // On copy action of correct text
    private fun switchExperiment()
    {
        val externalStorageDir = getExternalFilesDir(Environment.getDataDirectory().absolutePath)?.absolutePath
        current_exp_metrics.saveToJSON(this, "user"+ current_exp_metrics.userID + "_expid" + current_exp_metrics.expID, externalStorageDir)

        // TODO: End experiment!
        if (experimentList.size == 1)
        {
            Toast.makeText(
                this, R.string.exp_end_temp,
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Update list
        experimentList.removeAt(current_exp_id)

        // Get next id
        current_exp_id = (0..(experimentList.size - 1)).random()

        setupExperiment(current_exp_id)
    }

    private fun setupExperiment(experiment_id: Int)
    {
        current_exp_metrics = ExperimentMetrics(userID, experimentList[current_exp_id].id, System.currentTimeMillis())

        test_text.text = SpannableStringBuilder(experimentList[experiment_id].selectText)
        val st = experimentList[experiment_id].selectText.indexOf(experimentList[experiment_id].tgtText)
        val end = st + experimentList[experiment_id].tgtText.length
        test_text.text.setSpan(ForegroundColorSpan(Color.MAGENTA), st, end, 0)

        if(experimentList[experiment_id].expType == ExperimentType.GYROTEXT)
        {
            copyBut.visibility = View.GONE
            methodText.text = SpannableStringBuilder(resources.getString(R.string.tilt_method))
        }
        else
        {
            copyBut.visibility = View.VISIBLE
            methodText.text = SpannableStringBuilder(resources.getString(R.string.default_method))
        }
    }

    private fun startPractice()
    {
        // Assign ID
        userID = idInput.text.toString().toInt()

        // Update screen
        test_text.visibility = View.VISIBLE
        idBut.visibility = View.GONE
        idInput.visibility = View.GONE
        startPracticeBut.visibility = View.VISIBLE
        copyBut.visibility = View.VISIBLE
    }

    private fun startExperiment()
    {
        // Update screen
        startPracticeBut.visibility = View.GONE
        methodText.visibility = View.VISIBLE

        practiceFlag = false
        current_exp_id = (0..(experimentList.size - 1)).random()
        setupExperiment(current_exp_id)
    }
}
