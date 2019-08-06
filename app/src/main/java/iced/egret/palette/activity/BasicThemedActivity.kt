package iced.egret.palette.activity

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import com.jaredrummler.cyanea.CyaneaResources
import com.jaredrummler.cyanea.app.BaseCyaneaActivity
import com.jaredrummler.cyanea.delegate.CyaneaDelegate
import iced.egret.palette.R

/**
 * Automatically changes colorPrimary, colorPrimaryDark, colorAccent,
 * which styles elements that refer to those things implicitly and explicitly, like
 * buttons, toolbars, FABs, and switches.
 *
 * Text and toolbar icons aren't styled and have to be manually done in each activity/fragment.
 * Styling should be done onResume(), to reflect changes ASAP in most cases, regardless of where
 * changes were set.
 */
abstract class BasicThemedActivity : BaseActivity(), BaseCyaneaActivity {

    private val delegate: CyaneaDelegate by lazy {
        CyaneaDelegate.create(this, cyanea, getThemeResId())
    }

    private val resources: CyaneaResources by lazy {
        CyaneaResources(super.getResources(), cyanea)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(delegate.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        delegate.onCreate(savedInstanceState)
        super.onCreate(savedInstanceState)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        delegate.onPostCreate(savedInstanceState)

        // Cyanea messes with the window background onPostCreate(), which needs to be transparent
        // to show the previous activity on slide gesture via Slidr, so set it back.
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun onStart() {
        super.onStart()
        delegate.onStart()
    }

    override fun onResume() {
        super.onResume()
        delegate.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        delegate.onCreateOptionsMenu(menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun getResources(): Resources = resources

    /**
     * Change the theme in response to user action.
     */
    fun applyTheme(primary: Int? = null, accent: Int? = null,
                   text: Int? = null) {

        cyanea.edit {
            primary(primary ?: getColor(R.string.primary_color_key, R.color.cyanea_primary_reference))
            accent(accent ?: getColor(R.string.accent_color_key, R.color.cyanea_accent_reference))
        }.recreate(this)

    }

    private fun getColor(resource: Int, defaultColor: Int): Int {
        return defSharedPreferences.getInt(getString(resource), defaultColor)
    }

}