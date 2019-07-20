package iced.egret.palette.util

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.content.ContextCompat
import iced.egret.palette.R

/**
 * Handles color-related operations.
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

    private val themeColorResources = listOf(
            R.color.dodger_blue,
            R.color.sea_green,
            R.color.tangerine,
            R.color.lynch,
            R.color.coffee,
            R.color.idle_pink,
            R.color.boppin_blue,
            R.color.astral_yellow,
            R.color.myriad_magenta,
            R.color.resolute_cyan,
            R.color.fashion_red
    )
    // maps color ints to resources
    val colorResId = mutableMapOf<Int, Int>()

    var black: Int? = null
    var white: Int? = null
    var currentDrawableColor: Int? = null

    fun setup(context: Context) {
        loadColorsFromResources(context)
        currentDrawableColor = white
    }

    private fun loadColorsFromResources(context: Context) {
        for (i in 0 until themeColorResources.size) {
            val resId = themeColorResources[i]
            val colorInt = ContextCompat.getColor(context, resId)
            colorResId[colorInt] = resId
        }
        black = ContextCompat.getColor(context, R.color.black)
        white = ContextCompat.getColor(context, R.color.white)
    }

    /**
     * Changes the color of object to a predetermined color.
     */
    fun paintDrawable(drawable: Drawable?) {
        drawable?.mutate()  // make drawable not share state with others
        drawable?.setTint(currentDrawableColor as Int)
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