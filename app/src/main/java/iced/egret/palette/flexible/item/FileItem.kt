package iced.egret.palette.flexible.item

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import iced.egret.palette.R
import iced.egret.palette.flexible.viewholder.FileViewHolder
import java.io.File

class FileItem(val file: File, checked: Boolean) : AbstractFlexibleItem<FileViewHolder>() {

    private var viewHolder: FileViewHolder? = null
    var isChecked: Boolean = checked
        private set

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
                                holder: FileViewHolder,
                                position: Int,
                                payloads: MutableList<Any>?) {

        viewHolder = holder
        holder.textView.text = file.path
        holder.checkBox.isChecked = isChecked
    }

    override fun equals(other: Any?): Boolean {
        if (other !is FileItem) return false
        return this.file == other.file
    }

    override fun hashCode(): Int {
        return file.hashCode()
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): FileViewHolder {
        return FileViewHolder(view, adapter)
    }

    override fun getLayoutRes(): Int {
        return R.layout.item_file
    }

    fun toggleChecked() {
        val current = viewHolder?.checkBox?.isChecked ?: return
        isChecked = !current
        viewHolder?.checkBox?.isChecked = !current
    }

}