package iced.egret.palette.activity

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceFragmentCompat
import com.kizitonwose.colorpreferencecompat.ColorPreferenceCompat
import iced.egret.palette.R
import kotlinx.android.synthetic.main.appbar.*

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        buildToolbar()
        styleToolbar(toolbar)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
    }

    private fun buildToolbar() {
        toolbar.title = getString(R.string.title_activity_settings)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        var primaryColor: Int? = null
        var accentColor: Int? = null
        var toolbarItemColor: Int? = null

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            // Setting background color in XML doesn't work
            val view = super.onCreateView(inflater, container, savedInstanceState)
            view!!.setBackgroundColor(Color.WHITE)
            return view
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            setOnClickListeners()
        }

        private fun setOnClickListeners() {

            val primaryColorPref = findColorPreference(getString(R.string.primary_color_key))
            val accentColorPref = findColorPreference(getString(R.string.accent_color_key))
            val toolbarItemColorPref = findColorPreference(getString(R.string.toolbar_item_color_key))

            primaryColor = primaryColorPref.value
            accentColor = accentColorPref.value
            toolbarItemColor = toolbarItemColorPref.value

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
            toolbarItemColorPref.setOnPreferenceChangeListener { _, newValue ->
                if (newValue is Int) {
                    toolbarItemColor = newValue
                    applyTheme()
                }
                true
            }
        }

        private fun findColorPreference(key: String): ColorPreferenceCompat {
            return findPreference<ColorPreferenceCompat>(key)!!
        }

        private fun applyTheme() {
            (this.activity!! as BaseActivity).applyTheme(primaryColor, accentColor, toolbarItemColor)
            (this.activity!! as BaseActivity).styleToolbar(toolbar)
        }

    }

}