package iced.egret.palette.fragment

import android.graphics.Color
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import iced.egret.palette.R
import kotlinx.android.synthetic.main.appbar_list_fragment.view.*

/**
 * A Fragment that generally shows a list of objects, and shares its display with other such
 * Fragments in a single Activity. List objects are selectable. Toolbar has a TextView inside
 * (allowing for more granular control of the title's display), so it has to be themed manually,
 * unlike more basic Toolbars.
 *
 * Toolbar menu items are themed automatically. ActionMode's are not.
 */
abstract class ListFragment : Fragment() {

    protected lateinit var mToolbar: Toolbar

    abstract fun setClicksBlocked(doBlock: Boolean)
    abstract fun onAllFragmentsCreated()
    abstract fun onBackPressed(): Boolean

    private fun setToolbarTextColor() {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.context)
        val color = sharedPrefs!!.getInt(getString(R.string.toolbar_item_color_key), Color.WHITE)
        mToolbar.toolbarTitle.setTextColor(color)
    }

    override fun onResume() {
        super.onResume()
        // Allow updating whenever returning to fragment from Settings
        setToolbarTextColor()
    }

}