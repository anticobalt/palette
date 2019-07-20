package iced.egret.palette.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.preference.PreferenceFragmentCompat
import com.kizitonwose.colorpreferencecompat.ColorPreferenceCompat
import iced.egret.palette.R

class SettingsActivity : BaseActivity() {

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

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            setOnClickListeners()
        }

        private fun setOnClickListeners() {
            val colorPreference = findPreference<ColorPreferenceCompat>(getString(R.string.color_key))
            colorPreference?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue is Int) applyTheme(newValue)
                true
            }
        }

        private fun applyTheme(colorInt: Int) {
            (this.activity!! as BaseActivity).applyTheme(colorInt)
        }

    }

}