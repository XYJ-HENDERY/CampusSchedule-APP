package com.example.campusschedule.ui.schedule

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.FrameLayout
import kotlin.math.abs

class WeekSwipeLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    var onSwipe: ((Int) -> Unit)? = null

    private val touchSlop = (20 * resources.displayMetrics.density).toInt()
    private var startX = 0f
    private var startY = 0f
    private var swiping = false

    private val gd = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            if (abs(vx) > abs(vy) && abs(vx) > 150) {
                onSwipe?.invoke(if (vx < 0) 1 else -1)
                return true
            }
            return false
        }
    })

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.x; startY = ev.y; swiping = false
                gd.onTouchEvent(ev)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - startX
                val dy = ev.y - startY
                if (!swiping && abs(dx) > touchSlop && abs(dx) > abs(dy) * 1.2f) {
                    swiping = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    val down = MotionEvent.obtain(ev)
                    down.action = MotionEvent.ACTION_DOWN
                    gd.onTouchEvent(down)
                    down.recycle()
                    return true
                }
            }
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (swiping) {
            gd.onTouchEvent(ev)
            if (ev.actionMasked == MotionEvent.ACTION_UP ||
                ev.actionMasked == MotionEvent.ACTION_CANCEL
            ) swiping = false
            return true
        }
        return false
    }
}