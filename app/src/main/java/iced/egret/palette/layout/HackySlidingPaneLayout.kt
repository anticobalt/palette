package iced.egret.palette.layout

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import iced.egret.palette.R

/**
 * Adds features to SlidingPaneLayout (specific to the app).
 *
 * #1
 * ------
 * Alerts listener when isOpen() is actually valid.
 * isOpen() depends on mCanSlide, which is false up until at least onDraw(), because
 * you can't *technically* open something that doesn't exist I guess.
 * onDraw() happens after all fragment/activity lifecycle functions.
 *
 * Downside is that onDraw() is called many times.
 *
 * #2
 * ------
 * Normal SlidingPaneLayout intercepts all drags that originate from left edge, even while it is
 * open. This prevents dragging open nested SlidingPaneLayouts from the very left side.
 *
 * This class fixes the issue by unconditionally refusing to intercept all ACTION_MOVE events that
 * are inside the nested SlidingPaneLayout and happen while this layout is open.
 *
 */
class HackySlidingPaneLayout(context: Context, attributeSet: AttributeSet? = null)
    : SlidingPaneLayout(context, attributeSet) {

    interface HackyPanelSlideListener : PanelSlideListener {
        fun onSlidingPanelReady()
    }

    private var exposedListener: HackyPanelSlideListener? = null

    override fun setPanelSlideListener(listener: PanelSlideListener?) {
        super.setPanelSlideListener(listener)
        if (listener is HackyPanelSlideListener) exposedListener = listener
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        exposedListener?.onSlidingPanelReady()
    }

    /**
     * Only call super.onInterceptTouchEvent() if have to, because it does some state changes
     * that causes the panel to close if you start at the left edge, drag right, then drag back left,
     * instead of letting nested SlidingPaneLayout handle the entire process.
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked
        val bound = findViewById<SlidingPaneLayout?>(R.id.slider)?.right
                ?: return super.onInterceptTouchEvent(ev)

        return if (isOpen && action == MotionEvent.ACTION_MOVE && ev.x < bound) {
            false // don't intercept, let child handle
        } else super.onInterceptTouchEvent(ev)
    }

}