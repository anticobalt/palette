package iced.egret.palette.delegate

import android.content.Context
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import eu.davidea.flexibleadapter.FlexibleAdapter
import iced.egret.palette.fragment.CollectionViewFragment
import iced.egret.palette.model.Coverable

/**
 * Used to differentiate actions available depending on the collection type being viewed by
 * CollectionViewFragment. When action is complete, it notifies the CollectionViewFragment that is
 * listening.
 *
 * Delegate used instead of subtypes of CollectionViewFragment because replacing fragments
 * on-the-fly is taxing and causes UI stuttering.
 */
abstract class CollectionViewDelegate {

    data class ActionAlert(val message: String, val success: Boolean)

    var listener: CollectionViewFragment? = null

    abstract fun onBuildToolbar()
    abstract fun onCreateActionMode(mode: ActionMode, menu: Menu, selectedContentType: String): Boolean
    abstract fun onActionItemClicked(mode: ActionMode, item: MenuItem, adapter: FlexibleAdapter<*>,
                                     context: Context, selectedContentType: String): Boolean

    abstract fun onDestroyActionMode(mode: ActionMode)
    abstract fun onOptionsItemSelected(item: MenuItem): Boolean
    abstract fun onFabClick(context: Context, contents: List<Coverable>)

    protected fun alert(actionAlert: ActionAlert) {
        listener?.onDelegateAlert(actionAlert)
    }

}