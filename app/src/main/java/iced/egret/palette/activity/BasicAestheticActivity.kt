package iced.egret.palette.activity

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceManager
import com.afollestad.aesthetic.Aesthetic
import iced.egret.palette.R

abstract class BasicAestheticActivity: BaseActivity() {

    // Assume theme color can't be white
    private val invalidColor = -1

    private lateinit var sharedPreferences : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        Aesthetic.attach(this)
        super.onCreate(savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        applyTheme()
    }

    override fun onResume() {
        super.onResume()
        Aesthetic.resume(this)
    }

    override fun onPause() {
        super.onPause()
        Aesthetic.pause(this)
    }

    fun applyTheme(colorInt: Int? = null) {
        val primaryColor = colorInt ?: sharedPreferences.getInt(getString(R.string.color_key), invalidColor)
        if (primaryColor == invalidColor) return
        Aesthetic.config {
            colorPrimary(literal = primaryColor)
        }
    }

}