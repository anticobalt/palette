package iced.egret.palette.delegate

import android.content.Context
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import eu.davidea.flexibleadapter.FlexibleAdapter
import iced.egret.palette.model.Coverable

class FolderViewDelegate : CollectionViewDelegate() {

    override fun onBuildToolbar() {
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu, selectedContentType: String): Boolean {
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem, adapter: FlexibleAdapter<*>,
                                     context: Context, selectedContentType: String): Boolean {
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        // TODO
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return true
    }

    override fun onFabClick(context: Context, contents: List<Coverable>) {
        // TODO
    }

}