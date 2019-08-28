package com.digiclock.ui.widget

import android.annotation.TargetApi
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

import androidx.core.content.ContextCompat

import com.digiclock.R

class CircleSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var mWheelPaint: Paint? = null

    private var mReachedPaint: Paint? = null

    private var mReachedEdgePaint: Paint? = null

    private var mPointerPaint: Paint? = null

    private var mMaxProcess: Int = 0
    private var mCurProcess: Int = 0
    private var mUnreachedRadius: Float = 0.toFloat()
    private var mReachedColor: Int = 0
    private var mUnreachedColor: Int = 0
    private var mReachedWidth: Float = 0.toFloat()
    private var mUnreachedWidth: Float = 0.toFloat()
    private var isHasReachedCornerRound: Boolean = false
    private var mPointerColor: Int = 0
    private var mPointerRadius: Float = 0.toFloat()

    private var mCurAngle: Double = 0.toDouble()
    private var mWheelCurX: Float = 0.toFloat()
    private var mWheelCurY: Float = 0.toFloat()

    var isHasWheelShadow: Boolean = false
        private set
    var isHasPointerShadow: Boolean = false
        private set
    var wheelShadowRadius: Float = 0.toFloat()
        private set
    private var mPointerShadowRadius: Float = 0.toFloat()

    private var isHasCache: Boolean = false
    private var mCacheCanvas: Canvas? = null
    private var mCacheBitmap: Bitmap? = null

    private var isCanTouch: Boolean = false

    private var isScrollOneCircle: Boolean = false

    private var mDefShadowOffset: Float = 0.toFloat()

    private var mChangListener: OnSeekBarChangeListener? = null

    private val circleWidth: Float
        get() = Math.max(mUnreachedWidth, Math.max(mReachedWidth, mPointerRadius))

    private val selectedValue: Int
        get() = Math.round(mMaxProcess * (mCurAngle.toFloat() / 360))

    var curProcess: Int
        get() = mCurProcess
        set(curProcess) {
            this.mCurProcess = if (curProcess > mMaxProcess) mMaxProcess else curProcess
            if (mChangListener != null) {
                mChangListener!!.onChanged(this, curProcess)
            }
            refershPosition()
            invalidate()
        }

    var maxProcess: Int
        get() = mMaxProcess
        set(maxProcess) {
            mMaxProcess = maxProcess
            refershPosition()
            invalidate()
        }

    var reachedColor: Int
        get() = mReachedColor
        set(reachedColor) {
            this.mReachedColor = reachedColor
            mReachedPaint!!.color = reachedColor
            mReachedEdgePaint!!.color = reachedColor
            invalidate()
        }

    var unreachedColor: Int
        get() = mUnreachedColor
        set(unreachedColor) {
            this.mUnreachedColor = unreachedColor
            mWheelPaint!!.color = unreachedColor
            invalidate()
        }

    var reachedWidth: Float
        get() = mReachedWidth
        set(reachedWidth) {
            this.mReachedWidth = reachedWidth
            mReachedPaint!!.strokeWidth = reachedWidth
            mReachedEdgePaint!!.strokeWidth = reachedWidth
            invalidate()
        }

    var unreachedWidth: Float
        get() = mUnreachedWidth
        set(unreachedWidth) {
            this.mUnreachedWidth = unreachedWidth
            mWheelPaint!!.strokeWidth = unreachedWidth
            refershUnreachedWidth()
            invalidate()
        }

    var pointerColor: Int
        get() = mPointerColor
        set(pointerColor) {
            this.mPointerColor = pointerColor
            mPointerPaint!!.color = pointerColor
        }

    var pointerRadius: Float
        get() = mPointerRadius
        set(pointerRadius) {
            this.mPointerRadius = pointerRadius
            mPointerPaint!!.strokeWidth = pointerRadius
            invalidate()
        }

    var pointerShadowRadius: Float
        get() = mPointerShadowRadius
        set(pointerShadowRadius) {
            this.mPointerShadowRadius = pointerShadowRadius
            if (mPointerShadowRadius == 0f) {
                isHasPointerShadow = false
                mPointerPaint!!.clearShadowLayer()
            } else {
                mPointerPaint!!.setShadowLayer(
                    pointerShadowRadius,
                    mDefShadowOffset,
                    mDefShadowOffset,
                    Color.DKGRAY
                )
                setSoftwareLayer()
            }
            invalidate()
        }

    init {

        initAttrs(attrs, defStyleAttr)
        initPadding()
        initPaints()
    }

    private fun initPaints() {
        mDefShadowOffset = getDimen(R.dimen.def_shadow_offset)

        mWheelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mWheelPaint!!.color = mUnreachedColor
        mWheelPaint!!.style = Paint.Style.STROKE
        mWheelPaint!!.strokeWidth = mUnreachedWidth
        if (isHasWheelShadow) {
            mWheelPaint!!.setShadowLayer(
                wheelShadowRadius,
                mDefShadowOffset,
                mDefShadowOffset,
                Color.DKGRAY
            )
        }

        mReachedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mReachedPaint!!.color = mReachedColor
        mReachedPaint!!.style = Paint.Style.STROKE
        mReachedPaint!!.strokeWidth = mReachedWidth
        if (isHasReachedCornerRound) {
            mReachedPaint!!.strokeCap = Paint.Cap.ROUND
        }

        mPointerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPointerPaint!!.color = mPointerColor
        mPointerPaint!!.style = Paint.Style.FILL
        if (isHasPointerShadow) {
            mPointerPaint!!.setShadowLayer(
                mPointerShadowRadius,
                mDefShadowOffset,
                mDefShadowOffset,
                Color.DKGRAY
            )
        }

        mReachedEdgePaint = Paint(mReachedPaint)
        mReachedEdgePaint!!.style = Paint.Style.FILL
    }

    private fun initAttrs(attrs: AttributeSet?, defStyle: Int) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.CircleSeekBar, defStyle, 0)
        mMaxProcess = a.getInt(R.styleable.CircleSeekBar_wheel_max_process, 100)
        mCurProcess = a.getInt(R.styleable.CircleSeekBar_wheel_cur_process, 0)
        if (mCurProcess > mMaxProcess) mCurProcess = mMaxProcess
        mReachedColor = a.getColor(
            R.styleable.CircleSeekBar_wheel_reached_color,
            getColor(R.color.def_reached_color)
        )
        mUnreachedColor = a.getColor(
            R.styleable.CircleSeekBar_wheel_unreached_color,
            getColor(R.color.def_wheel_color)
        )
        mUnreachedWidth = a.getDimension(
            R.styleable.CircleSeekBar_wheel_unreached_width,
            getDimen(R.dimen.def_wheel_width)
        )
        isHasReachedCornerRound =
            a.getBoolean(R.styleable.CircleSeekBar_wheel_reached_has_corner_round, true)
        mReachedWidth =
            a.getDimension(R.styleable.CircleSeekBar_wheel_reached_width, mUnreachedWidth)
        mPointerColor = a.getColor(
            R.styleable.CircleSeekBar_wheel_pointer_color,
            getColor(R.color.def_pointer_color)
        )
        mPointerRadius =
            a.getDimension(R.styleable.CircleSeekBar_wheel_pointer_radius, mReachedWidth / 2)
        isHasWheelShadow = a.getBoolean(R.styleable.CircleSeekBar_wheel_has_wheel_shadow, false)
        if (isHasWheelShadow) {
            wheelShadowRadius = a.getDimension(
                R.styleable.CircleSeekBar_wheel_shadow_radius,
                getDimen(R.dimen.def_shadow_radius)
            )
        }
        isHasPointerShadow = a.getBoolean(R.styleable.CircleSeekBar_wheel_has_pointer_shadow, false)
        if (isHasPointerShadow) {
            mPointerShadowRadius = a.getDimension(
                R.styleable.CircleSeekBar_wheel_pointer_shadow_radius,
                getDimen(R.dimen.def_shadow_radius)
            )
        }
        isHasCache = a.getBoolean(R.styleable.CircleSeekBar_wheel_has_cache, isHasWheelShadow)
        isCanTouch = a.getBoolean(R.styleable.CircleSeekBar_wheel_can_touch, true)
        isScrollOneCircle =
            a.getBoolean(R.styleable.CircleSeekBar_wheel_scroll_only_one_circle, false)

        if (isHasPointerShadow or isHasWheelShadow) {
            setSoftwareLayer()
        }
        a.recycle()
    }

    private fun initPadding() {
        val paddingLeft = paddingLeft
        val paddingTop = paddingTop
        val paddingRight = paddingRight
        val paddingBottom = paddingBottom
        var paddingStart = 0
        var paddingEnd = 0
        if (Build.VERSION.SDK_INT >= 17) {
            paddingStart = getPaddingStart()
            paddingEnd = getPaddingEnd()
        }
        val maxPadding = Math.max(
            paddingLeft, Math.max(
                paddingTop,
                Math.max(paddingRight, Math.max(paddingBottom, Math.max(paddingStart, paddingEnd)))
            )
        )
        setPadding(maxPadding, maxPadding, maxPadding, maxPadding)
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun getColor(colorId: Int): Int {
        val version = Build.VERSION.SDK_INT
        return if (version >= 23) {
            context.getColor(colorId)
        } else {
            ContextCompat.getColor(context, colorId)
        }
    }

    private fun getDimen(dimenId: Int): Float {
        return resources.getDimension(dimenId)
    }

    private fun setSoftwareLayer() {
        if (Build.VERSION.SDK_INT >= 11) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = View.getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        val width = View.getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val min = Math.min(width, height)
        setMeasuredDimension(min, min)

        refershPosition()
        refershUnreachedWidth()
    }

    override fun onDraw(canvas: Canvas) {
        val left = paddingLeft + mUnreachedWidth / 2
        val top = paddingTop + mUnreachedWidth / 2
        val right = canvas.width.toFloat() - paddingRight.toFloat() - mUnreachedWidth / 2
        val bottom = canvas.height.toFloat() - paddingBottom.toFloat() - mUnreachedWidth / 2
        val centerX = (left + right) / 2
        val centerY = (top + bottom) / 2

        val wheelRadius = (canvas.width - paddingLeft - paddingRight) / 2 - mUnreachedWidth / 2

        if (isHasCache) {
            if (mCacheCanvas == null) {
                buildCache(centerX, centerY, wheelRadius)
            }
            canvas.drawBitmap(mCacheBitmap!!, 0f, 0f, null)
        } else {
            canvas.drawCircle(centerX, centerY, wheelRadius, mWheelPaint!!)
        }

        canvas.drawArc(
            RectF(left, top, right, bottom),
            -90f,
            mCurAngle.toFloat(),
            false,
            mReachedPaint!!
        )


        canvas.drawCircle(mWheelCurX, mWheelCurY, mPointerRadius, mPointerPaint!!)
    }

    private fun buildCache(centerX: Float, centerY: Float, wheelRadius: Float) {
        mCacheBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        mCacheCanvas = Canvas(mCacheBitmap!!)


        mCacheCanvas!!.drawCircle(centerX, centerY, wheelRadius, mWheelPaint!!)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        if (isCanTouch && (event.action == MotionEvent.ACTION_MOVE || isTouch(x, y))) {

            var cos = computeCos(x, y)

            val angle: Double
            if (x < width / 2) {
                angle = Math.PI * RADIAN + Math.acos(cos.toDouble()) * RADIAN
            } else {
                angle = Math.PI * RADIAN - Math.acos(cos.toDouble()) * RADIAN
            }
            if (isScrollOneCircle) {
                if (mCurAngle > 270 && angle < 90) {
                    mCurAngle = 360.0
                    cos = -1f
                } else if (mCurAngle < 90 && angle > 270) {
                    mCurAngle = 0.0
                    cos = -1f
                } else {
                    mCurAngle = angle
                }
            } else {
                mCurAngle = angle
            }
            mCurProcess = selectedValue
            refershWheelCurPosition(cos.toDouble())
            if (mChangListener != null && event.action and (MotionEvent.ACTION_MOVE or MotionEvent.ACTION_UP) > 0) {
                mChangListener!!.onChanged(this, mCurProcess)
            }
            invalidate()
            return true
        } else {
            return super.onTouchEvent(event)
        }
    }

    private fun isTouch(x: Float, y: Float): Boolean {
        val radius = ((width - paddingLeft - paddingRight + circleWidth) / 2).toDouble()
        val centerX = (width / 2).toDouble()
        val centerY = (height / 2).toDouble()
        return Math.pow(centerX - x, 2.0) + Math.pow(centerY - y, 2.0) < radius * radius
    }

    private fun refershUnreachedWidth() {
        mUnreachedRadius =
            (measuredWidth.toFloat() - paddingLeft.toFloat() - paddingRight.toFloat() - mUnreachedWidth) / 2
    }

    private fun refershWheelCurPosition(cos: Double) {
        mWheelCurX = calcXLocationInWheel(mCurAngle, cos)
        mWheelCurY = calcYLocationInWheel(cos)
    }

    private fun refershPosition() {
        mCurAngle = mCurProcess.toDouble() / mMaxProcess * 360.0
        val cos = -Math.cos(Math.toRadians(mCurAngle))
        refershWheelCurPosition(cos)
    }

    private fun calcXLocationInWheel(angle: Double, cos: Double): Float {
        return if (angle < 180) {
            (measuredWidth / 2 + Math.sqrt(1 - cos * cos) * mUnreachedRadius).toFloat()
        } else {
            (measuredWidth / 2 - Math.sqrt(1 - cos * cos) * mUnreachedRadius).toFloat()
        }
    }

    private fun calcYLocationInWheel(cos: Double): Float {
        return measuredWidth / 2 + mUnreachedRadius * cos.toFloat()
    }


    private fun computeCos(x: Float, y: Float): Float {
        val width = x - width / 2
        val height = y - height / 2
        val slope = Math.sqrt((width * width + height * height).toDouble()).toFloat()
        return height / slope
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        bundle.putParcelable(INATANCE_STATE, super.onSaveInstanceState())
        bundle.putInt(INSTANCE_MAX_PROCESS, mMaxProcess)
        bundle.putInt(INSTANCE_CUR_PROCESS, mCurProcess)
        bundle.putInt(INSTANCE_REACHED_COLOR, mReachedColor)
        bundle.putFloat(INSTANCE_REACHED_WIDTH, mReachedWidth)
        bundle.putBoolean(INSTANCE_REACHED_CORNER_ROUND, isHasReachedCornerRound)
        bundle.putInt(INSTANCE_UNREACHED_COLOR, mUnreachedColor)
        bundle.putFloat(INSTANCE_UNREACHED_WIDTH, mUnreachedWidth)
        bundle.putInt(INSTANCE_POINTER_COLOR, mPointerColor)
        bundle.putFloat(INSTANCE_POINTER_RADIUS, mPointerRadius)
        bundle.putBoolean(INSTANCE_POINTER_SHADOW, isHasPointerShadow)
        bundle.putFloat(INSTANCE_POINTER_SHADOW_RADIUS, mPointerShadowRadius)
        bundle.putBoolean(INSTANCE_WHEEL_SHADOW, isHasWheelShadow)
        bundle.putFloat(INSTANCE_WHEEL_SHADOW_RADIUS, mPointerShadowRadius)
        bundle.putBoolean(INSTANCE_WHEEL_HAS_CACHE, isHasCache)
        bundle.putBoolean(INSTANCE_WHEEL_CAN_TOUCH, isCanTouch)
        bundle.putBoolean(INSTANCE_WHEEL_SCROLL_ONLY_ONE_CIRCLE, isScrollOneCircle)
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            super.onRestoreInstanceState(state.getParcelable(INATANCE_STATE))
            mMaxProcess = state.getInt(INSTANCE_MAX_PROCESS)
            mCurProcess = state.getInt(INSTANCE_CUR_PROCESS)
            mReachedColor = state.getInt(INSTANCE_REACHED_COLOR)
            mReachedWidth = state.getFloat(INSTANCE_REACHED_WIDTH)
            isHasReachedCornerRound = state.getBoolean(INSTANCE_REACHED_CORNER_ROUND)
            mUnreachedColor = state.getInt(INSTANCE_UNREACHED_COLOR)
            mUnreachedWidth = state.getFloat(INSTANCE_UNREACHED_WIDTH)
            mPointerColor = state.getInt(INSTANCE_POINTER_COLOR)
            mPointerRadius = state.getFloat(INSTANCE_POINTER_RADIUS)
            isHasPointerShadow = state.getBoolean(INSTANCE_POINTER_SHADOW)
            mPointerShadowRadius = state.getFloat(INSTANCE_POINTER_SHADOW_RADIUS)
            isHasWheelShadow = state.getBoolean(INSTANCE_WHEEL_SHADOW)
            mPointerShadowRadius = state.getFloat(INSTANCE_WHEEL_SHADOW_RADIUS)
            isHasCache = state.getBoolean(INSTANCE_WHEEL_HAS_CACHE)
            isCanTouch = state.getBoolean(INSTANCE_WHEEL_CAN_TOUCH)
            isScrollOneCircle = state.getBoolean(INSTANCE_WHEEL_SCROLL_ONLY_ONE_CIRCLE)
            initPaints()
        } else {
            super.onRestoreInstanceState(state)
        }

        if (mChangListener != null) {
            mChangListener!!.onChanged(this, mCurProcess)
        }
    }

    fun isHasReachedCornerRound(): Boolean {
        return isHasReachedCornerRound
    }

    fun setHasReachedCornerRound(hasReachedCornerRound: Boolean) {
        isHasReachedCornerRound = hasReachedCornerRound
        mReachedPaint!!.strokeCap = if (hasReachedCornerRound) Paint.Cap.ROUND else Paint.Cap.BUTT
        invalidate()
    }

    fun setWheelShadow(wheelShadow: Float) {
        this.wheelShadowRadius = wheelShadow
        if (wheelShadow == 0f) {
            isHasWheelShadow = false
            mWheelPaint!!.clearShadowLayer()
            mCacheCanvas = null
            mCacheBitmap!!.recycle()
            mCacheBitmap = null
        } else {
            mWheelPaint!!.setShadowLayer(
                wheelShadowRadius,
                mDefShadowOffset,
                mDefShadowOffset,
                Color.DKGRAY
            )
            setSoftwareLayer()
        }
        invalidate()
    }

    fun setOnSeekBarChangeListener(listener: OnSeekBarChangeListener) {
        mChangListener = listener
    }

    interface OnSeekBarChangeListener {
        fun onChanged(seekbar: CircleSeekBar, curValue: Int)
    }

    companion object {

        private val RADIAN = 180 / Math.PI

        private val INATANCE_STATE = "state"
        private val INSTANCE_MAX_PROCESS = "max_process"
        private val INSTANCE_CUR_PROCESS = "cur_process"
        private val INSTANCE_REACHED_COLOR = "reached_color"
        private val INSTANCE_REACHED_WIDTH = "reached_width"
        private val INSTANCE_REACHED_CORNER_ROUND = "reached_corner_round"
        private val INSTANCE_UNREACHED_COLOR = "unreached_color"
        private val INSTANCE_UNREACHED_WIDTH = "unreached_width"
        private val INSTANCE_POINTER_COLOR = "pointer_color"
        private val INSTANCE_POINTER_RADIUS = "pointer_radius"
        private val INSTANCE_POINTER_SHADOW = "pointer_shadow"
        private val INSTANCE_POINTER_SHADOW_RADIUS = "pointer_shadow_radius"
        private val INSTANCE_WHEEL_SHADOW = "wheel_shadow"
        private val INSTANCE_WHEEL_SHADOW_RADIUS = "wheel_shadow_radius"
        private val INSTANCE_WHEEL_HAS_CACHE = "wheel_has_cache"
        private val INSTANCE_WHEEL_CAN_TOUCH = "wheel_can_touch"
        private val INSTANCE_WHEEL_SCROLL_ONLY_ONE_CIRCLE = "wheel_scroll_only_one_circle"
    }
}