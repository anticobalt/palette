package iced.egret.palette.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.preference.PreferenceFragmentCompat
import com.kizitonwose.colorpreferencecompat.ColorPreferenceCompat
import iced.egret.palette.R

class SettingsActivity : BasicAestheticActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        var primaryColor : Int? = null
        var accentColor : Int? = null
        var textColor : Int? = null
        var bgColor : Int? = null

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            setOnClickListeners()
        }

        private fun setOnClickListeners() {
            val primaryColorPref = findPreference<ColorPreferenceCompat>(getString(R.string.primary_color_key))!!
            val accentColorPref = findPreference<ColorPreferenceCompat>(getString(R.string.accent_color_key))!!
            val textColorPref = findPreference<ColorPreferenceCompat>(getString(R.string.text_color_key))!!
            val bgColorPref = findPreference<ColorPreferenceCompat>(getString(R.string.bg_color_key))!!

            primaryColor = primaryColorPref.value
            accentColor = accentColorPref.value
            textColor = textColorPref.value
            bgColor = bgColorPref.value

            primaryColorPref.setOnPreferenceChangeListener { _, newValue ->
                if (newValue is Int) {
                    primaryColor = newValue
                    applyTheme()
                }
                true
            }
            accentColorPref.setOnPreferenceChangeListener { _, newValue ->
                if (newValue is Int) {
                    accentColor = newValue
                    applyTheme()
                }
                true
            }
            textColorPref.setOnPreferenceChangeListener { _, newValue ->
                if (newValue is Int) {
                    textColor = newValue
                    applyTheme()
                }
                true
            }
            bgColorPref.setOnPreferenceChangeListener { _, newValue ->
                if (newValue is Int) {
                    bgColor = newValue
                    applyTheme()
                }
                true
            }
        }

        private fun applyTheme() {
            (this.activity!! as BasicAestheticActivity).applyTheme(primaryColor, accentColor, textColor, bgColor)
        }

    }

}