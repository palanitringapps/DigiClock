package com.digiclock

import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.digiclock.ui.widget.CircleSeekBar
import com.digiclock.ui.widget.CustomDigitalClock
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var mHourSeekbar: CircleSeekBar
    private lateinit var mMinuteSeekbar: CircleSeekBar
    private lateinit var mTextView: CustomDigitalClock
    private lateinit var countTimer: CountDownTimer
    private lateinit var countTimerHour: CountDownTimer
    private var twelveHrsFormat: Boolean = true
    private var startMinute = 60
    private var startHour = 12

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_digiclock)

        mHourSeekbar = findViewById(R.id.seek_hour)
        mMinuteSeekbar = findViewById(R.id.seek_minute)
        mTextView = findViewById(R.id.textview)

        setCurrentTime()

        val change: AppCompatButton = findViewById(R.id.change)
        change.setOnClickListener {
            twelveHrsFormat = !twelveHrsFormat
            mTextView.setFormat(twelveHrsFormat)
            if (twelveHrsFormat) change.text = getString(R.string._24_hrs_format)
            else change.text = getString(R.string._12_hr_format)
        }

        mTextView.setFormat(twelveHrsFormat)
        setCurrentTime()

        mHourSeekbar.setOnTouchListener { p0, p1 -> true }

        mMinuteSeekbar.setOnTouchListener { p0, p1 -> true }
    }


    private fun setCurrentTime() {
        val calendar = Calendar.getInstance()
        setMinutes(calendar.get(Calendar.MINUTE))
        setHours(calendar.get(Calendar.HOUR))
        mMinuteSeekbar.curProcess = calendar.get(Calendar.MINUTE)
        mHourSeekbar.curProcess = calendar.get(Calendar.HOUR)

    }

    private fun setMinutes(currentMinute: Int) {
        var timerMinute = (startMinute - currentMinute).toLong()
        countTimer = object : CountDownTimer(timerMinute * 60000, 60000) {

            override fun onTick(millisUntilFinished: Long) {
                mMinuteSeekbar.curProcess = 60 - (millisUntilFinished / 60000).toInt()
            }

            override fun onFinish() {
                timerMinute = 60
                countTimer.start()
            }
        }
        countTimer.start()
    }

    private fun setHours(currentHour: Int) {
        var timerHours = (startHour - currentHour).toLong()
        countTimerHour = object : CountDownTimer(timerHours * 60 * 60000, 60 * 60000) {

            override fun onTick(millisUntilFinished: Long) {
                mHourSeekbar.curProcess = 12 - ((millisUntilFinished / (60 * 60000)).toInt())
            }

            override fun onFinish() {
                timerHours = 12
                countTimerHour.start()
            }
        }
        countTimerHour.start()
    }
}
