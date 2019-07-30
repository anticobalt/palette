package iced.egret.palette

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.slidingpanelayout.widget.SlidingPaneLayout

/**
 * Alerts listener when isOpen() is actually valid.
 * isOpen() depends on mCanSlide, which is false up until at least onDraw(), because
 * you can't *technically* open something that doesn't exist I guess.
 * onDraw() happens after all fragment/activity lifecycle functions.
 *
 * Downside is that onDraw() is called many times.
 */
class HackySlidingPaneLayout(context: Context, attributeSet: AttributeSet? = null)
    : SlidingPaneLayout(context, attributeSet) {

    interface HackyPanelSlideListener : PanelSlideListener {
        fun onSlidingPanelReady()
    }

    private var exposedListener : HackyPanelSlideListener? = null

    override fun setPanelSlideListener(listener: PanelSlideListener?) {
        super.setPanelSlideListener(listener)
        if (listener is HackyPanelSlideListener) exposedListener = listener
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        exposedListener?.onSlidingPanelReady()
    }

}