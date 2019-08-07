package iced.egret.palette.util

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.graphics.ColorUtils

/**
 * Handles color-related operations.
 */
object Painter {

    val DARKEN_SLIGHT = Color.rgb(200, 200, 200)
    val DARKEN_MODERATE = Color.rgb(100, 100, 100)

    private val validDarkenStrengths = setOf(
            null,
            DARKEN_SLIGHT,
            DARKEN_MODERATE
    )

    var currentDrawableColor = Color.WHITE

    /**
     * Changes the color of object to a predetermined color.
     */
    fun paintDrawable(drawable: Drawable?) {
        drawable?.mutate()  // make drawable not share state with others
        drawable?.setTint(currentDrawableColor)
    }

    /**
     * Make given ImageView darker.
     * https://stackoverflow.com/a/15896811
     */
    fun darken(imageView: ImageView, strength: Int?) {
        if (strength !in validDarkenStrengths) return

        if (strength == null) {
            imageView.colorFilter = null
        } else {
            imageView.setColorFilter(strength, android.graphics.PorterDuff.Mode.MULTIPLY)
        }
    }

    /**
     * Given a color that is to be the primary color, return an approximation of
     * the primary color dark version.
     * https://material.io/design/color/#color-theme-creation
     *
     * @param colorHex a string in form #RRGGBB or #AARRGGBB
     */
    fun getMaterialDark(colorHex: String) : String {
        val colorInt = Color.parseColor(colorHex)
        val colorDarkInt = getMaterialDark(colorInt)
        return Integer.toHexString(colorDarkInt)
    }

    fun getMaterialDark(colorInt: Int) : Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(colorInt, hsl)

        // Darken HSL by factor of 12, as specified in https://stackoverflow.com/a/40964456
        val materialDark700 = 12
        hsl[2] -= materialDark700 / 100f
        if (hsl[2] < 0)
            hsl[2] = 0f

        return ColorUtils.HSLToColor(hsl)
    }

}