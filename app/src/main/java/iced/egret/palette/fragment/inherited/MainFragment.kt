package iced.egret.palette.fragment.inherited

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import eu.davidea.flexibleadapter.FlexibleAdapter
import iced.egret.palette.R
import iced.egret.palette.activity.MainActivity
import kotlinx.android.synthetic.main.appbar_list_fragment.view.*

/**
 * A Fragment that generally shows a list of objects, and sits inside MainActivity along with other
 * MainFragments. List objects are selectable. Toolbar has a TextView inside (allowing for more
 * granular control of the title's display).
 *
 * Coloring has to be done in onResume to show up when returning from SettingsActivity.
 */
abstract class MainFragment : Fragment(), ActionMode.Callback,
        FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener {

    lateinit var toolbar: Toolbar
    lateinit var navigationDrawable: DrawerArrowDrawable
    private lateinit var sharedPrefs: SharedPreferences
    protected lateinit var mActivity: MainActivity

    abstract fun setClicksBlocked(doBlock: Boolean)
    abstract fun onAllFragmentsCreated()
    abstract fun onBackPressed(): Boolean

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mActivity = activity as MainActivity
        navigationDrawable = DrawerArrowDrawable(context!!)
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.context)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    /**
     * Only needs to be done on creation b/c SettingsActivity recreates activity when
     * theme changes.
     */
    protected open fun colorBars() {
        // Basic stuff
        mActivity.colorStandardElements(toolbar)

        // Advanced stuff
        val color = sharedPrefs.getInt(getString(R.string.toolbar_item_color_key), Color.WHITE)
        toolbar.toolbarTitle.setTextColor(color)
        navigationDrawable.color = color  // Only DrawerArrowDrawable's own color function works
    }

}