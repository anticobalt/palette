package iced.egret.palette.util

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.widget.ImageView
import iced.egret.palette.R

/**
 * Uniformly colors and darkens objects.
 * Requires setting up, ideally in the starting activity.
 */
object Painter {

    val DARKEN_SLIGHT = Color.rgb(200, 200, 200)
    val DARKEN_MODERATE = Color.rgb(100, 100, 100)

    private val validDarkenStrengths = setOf(
            null,
            DARKEN_SLIGHT,
            DARKEN_MODERATE
    )

    const val colorResource = R.color.white
    var color: Int? = null  // set by getColor() in an activity or fragment, using colorResource

    /**
     * Changes the color of object to a predetermined color.
     */
    fun paintDrawable(drawable: Drawable?) {
        drawable?.mutate()  // make drawable not share state with others
        drawable?.setTint(color as Int)
    }

    /**
     * Make given ImageView darker.
     * https://stackoverflow.com/a/15896811
     */
    fun darken(imageView: ImageView, strength: Int?) {
        if (strength !in validDarkenStrengths) return

        if (strength == null) {
            imageView.colorFilter = null
        }
        else {
            imageView.setColorFilter(strength, android.graphics.PorterDuff.Mode.MULTIPLY)
        }
    }

}