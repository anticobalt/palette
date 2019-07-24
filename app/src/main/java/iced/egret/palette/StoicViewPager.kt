package iced.egret.palette

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

/**
 * Ignores touch events that occur while the ViewPager is refreshed.
 * https://github.com/chrisbanes/PhotoView/issues/31#issuecomment-19803926
 */
class StoicViewPager(context: Context, attributeSet: AttributeSet? = null) : ViewPager(context, attributeSet) {

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        try {
            return super.onTouchEvent(ev)
        } catch (e : IllegalArgumentException) {
            e.printStackTrace()
        }
        return false
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        try {
            return super.onInterceptTouchEvent(ev)
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        }
        return false
    }

}