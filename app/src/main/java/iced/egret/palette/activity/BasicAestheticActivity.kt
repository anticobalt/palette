package iced.egret.palette.activity

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceManager
import com.afollestad.aesthetic.Aesthetic
import iced.egret.palette.R

abstract class BasicAestheticActivity: BaseActivity() {

    private lateinit var sharedPreferences : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        Aesthetic.attach(this)
        super.onCreate(savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onResume() {
        super.onResume()
        Aesthetic.resume(this)
    }

    override fun onPause() {
        super.onPause()
        Aesthetic.pause(this)
    }

    fun applyTheme(primary: Int? = null, accent: Int? = null,
                   text: Int? = null, bg: Int? = null) {

        val primaryColor = primary ?: getColor(R.string.primary_color_key, R.color.colorPrimary)
        val accentColor = accent ?: getColor(R.string.accent_color_key, R.color.colorAccent)
        val textColor = text ?: getColor(R.string.text_color_key, R.color.white)
        val bgColor = bg ?: getColor(R.string.bg_color_key, R.color.space)

        Aesthetic.config {
            colorPrimary(literal = primaryColor)
            colorAccent(literal = accentColor)

            // Only applies to whatever string is set as toolbar.title, supportActionBar or standalone
            toolbarTitleColor(literal = textColor)

            // Only applies to MenuItems + navigation icons on toolbars NOT set as supportActionBar
            toolbarIconColor(literal = textColor)
        }

    }

    private fun getColor(resource: Int, defaultColor: Int) : Int {
        return sharedPreferences.getInt(getString(resource), defaultColor)
    }

}