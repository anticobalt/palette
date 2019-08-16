package iced.egret.palette.activity

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.app.TaskStackBuilder
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import iced.egret.palette.R
import iced.egret.palette.activity.inherited.BaseActivity
import iced.egret.palette.activity.inherited.SlideActivity
import iced.egret.palette.util.DialogGenerator
import kotlinx.android.synthetic.main.appbar.*

class SettingsActivity : SlideActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        val themeKeys = listOf(
                R.string.key_primary_color,
                R.string.key_accent_color,
                R.string.key_toolbar_item_color
        ).map { id -> getString(id) }

        if (key != null && key in themeKeys) {
            // Rebuild all activities in stack to refresh theme
            // https://stackoverflow.com/a/28799124
            TaskStackBuilder.create(this)
                    .addNextIntent(Intent(this, MainActivity::class.java))
                    .addNextIntent(this.intent)
                    .startActivities()
            overridePendingTransition(0, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        buildToolbar()
        colorStandardElements(toolbar)
        defSharedPreferences.registerOnSharedPreferenceChangeListener(this)

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

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            // Setting background color in XML doesn't work
            val view = super.onCreateView(inflater, container, savedInstanceState)
            view!!.setBackgroundColor(Color.WHITE)
            return view
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val sdPref = findPreference<Preference>(getString(R.string.key_sd_card_grant))
            sdPref?.setOnPreferenceClickListener {
                DialogGenerator.grantSdCardPrompt(activity as BaseActivity)
                true
            }
        }

    }

}