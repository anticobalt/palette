package iced.egret.palette.util

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

object StateBuilder : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    fun build(context: Context, path: String?, callback: (() -> Unit)? = null) {
        if (callback == null) {
            // On UI thread
            Storage.setupIfRequired(context)
            CollectionManager.setup(path)
        } else {
            launch {
                // On IO thread, then do UI callback
                Storage.setupIfRequired(context)
                CollectionManager.setup(path)
                withContext(Dispatchers.Main) { callback() }
            }
        }
    }

}