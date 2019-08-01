package iced.egret.palette.fragment

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
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

    lateinit var toolbar: Toolbar
    lateinit var navigationDrawable : DrawerArrowDrawable
    protected lateinit var sharedPrefs : SharedPreferences

    abstract fun setClicksBlocked(doBlock: Boolean)
    abstract fun onAllFragmentsCreated()
    abstract fun onBackPressed(): Boolean

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        navigationDrawable = DrawerArrowDrawable(context!!)
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.context)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    protected open fun setToolbarItemColor() {
        val color = sharedPrefs.getInt(getString(R.string.toolbar_item_color_key), Color.WHITE)
        toolbar.toolbarTitle.setTextColor(color)

        navigationDrawable.color = color  // Only DrawerArrowDrawable's own color function works
    }

    override fun onResume() {
        super.onResume()
        // Allow updating whenever returning to fragment from Settings
        setToolbarItemColor()
    }

}