package iced.egret.palette.delegate

import android.content.Context
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import iced.egret.palette.delegate.inherited.CollectionViewDelegate
import iced.egret.palette.fragment.CollectionViewFragment
import iced.egret.palette.model.inherited.Collection
import iced.egret.palette.model.inherited.Coverable

class FolderViewDelegate : CollectionViewDelegate() {

    override fun onBuildToolbar(toolbar: Toolbar) {
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu, selectedContentType: String): Boolean {
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem, selectedContent: List<Coverable>,
                                     context: Context, selectedContentType: String): Boolean {
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        // TODO
    }

    override fun onOptionsItemSelected(item: MenuItem, fragment: CollectionViewFragment, currentCollection: Collection): Boolean {
        return true
    }

    override fun onFabClick(context: Context, contents: List<Coverable>) {
        // TODO
    }

}