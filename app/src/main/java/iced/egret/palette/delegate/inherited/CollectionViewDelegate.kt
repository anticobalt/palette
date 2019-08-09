package iced.egret.palette.delegate.inherited

import android.content.Context
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import eu.davidea.flexibleadapter.FlexibleAdapter
import iced.egret.palette.fragment.CollectionViewFragment
import iced.egret.palette.model.inherited.Collection
import iced.egret.palette.model.inherited.Coverable

/**
 * Used to differentiate actions available depending on the collection type being viewed by
 * CollectionViewFragment. When action is complete, it notifies the CollectionViewFragment that is
 * listening.
 *
 * Delegate used instead of subtypes of CollectionViewFragment because replacing fragments
 * on-the-fly is taxing and causes UI stuttering.
 */
abstract class CollectionViewDelegate {

    data class ActionAlert(val success: Boolean)

    var listener: CollectionViewFragment? = null

    abstract fun onBuildToolbar(toolbar: Toolbar)
    abstract fun onCreateActionMode(mode: ActionMode, menu: Menu, selectedContentType: String): Boolean
    abstract fun onActionItemClicked(mode: ActionMode, item: MenuItem, adapter: FlexibleAdapter<*>,
                                     context: Context, selectedContentType: String): Boolean

    abstract fun onDestroyActionMode(mode: ActionMode)
    abstract fun onOptionsItemSelected(item: MenuItem, fragment: CollectionViewFragment, currentCollection: Collection): Boolean
    abstract fun onFabClick(context: Context, contents: List<Coverable>)

    protected fun alert(actionAlert: ActionAlert) {
        listener?.onDelegateAlert(actionAlert)
    }

}