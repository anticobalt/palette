package iced.egret.palette.util

import android.graphics.drawable.Drawable
import iced.egret.palette.R

/**
 * Changes the color of objects to a constant color, to keep everything uniform.
 * Requires setting up, ideally in the starting activity.
 */
object Painter {

    const val colorResource = R.color.white
    var color: Int? = null  // set by getColor() in an activity or fragment, using colorResource

    fun paintDrawable(drawable: Drawable?) {
        drawable?.setTint(color as Int)
    }

}