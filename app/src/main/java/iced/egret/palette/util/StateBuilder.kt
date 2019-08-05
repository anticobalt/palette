package iced.egret.palette.util

import android.content.Context

object StateBuilder {

    fun build(context: Context, path: String?) {
        Storage.setupIfRequired(context)
        CollectionManager.setup(path)
        Painter.setup(context)
    }

}