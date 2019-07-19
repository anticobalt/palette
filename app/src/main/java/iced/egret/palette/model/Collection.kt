package iced.egret.palette.model

import android.net.Uri
import android.view.View
import com.bumptech.glide.Glide
import iced.egret.palette.R
import iced.egret.palette.recyclerview_component.CoverViewHolder

/***
 * Properties:
 * - name
 * - terminal
 * - cover
 * - icon
 * - pictures
 * - size
 * - totalSize
 */
abstract class Collection(override var name: String, val path: String) : Coverable {

    override val terminal = false
    override val cover = mutableMapOf<String, Any>(
            "id" to R.drawable.default_collection_cover
    )

    abstract var _pictures: MutableList<Picture>  // internal
    abstract val pictures: List<Picture>  // external

    abstract val contentsMap: Map<String, List<Coverable>>
    abstract val totalSize : Int
    var size = 0
        protected set

    fun isEmpty() : Boolean {
        return size == 0
    }

    fun isNotEmpty() : Boolean {
        return !isEmpty()
    }

    override fun toString(): String {
        return "$name, $size, $totalSize"
    }

    override fun loadCoverInto(holder: CoverViewHolder) {
        if (holder.ivItem != null) {
            setCoverUri()
            val imageReference : Any? = if (cover["uri"] != null) {
                cover["uri"]
            } else {
                cover["id"]
            }

            val glide =
                    Glide.with(holder.itemView.context)
                            .load(imageReference)
                            .centerCrop()

            // Load image with signature if possible
            buildGlideImage(glide, holder.ivItem, imageReference)

        }
        if (holder.tvItem != null) {
            holder.tvItem.visibility = View.VISIBLE
        }
    }


    abstract fun getContents() : List<Coverable>

    fun findPictureByPath(path: String) : Picture? {
        return _pictures.find {picture -> picture.filePath == path}
    }

    open fun addPicture(newPicture: Picture, toFront: Boolean = false, position: Int? = null) {
        when {
            toFront -> _pictures.add(0, newPicture)
            position != null -> _pictures.add(position, newPicture)
            else -> _pictures.add(newPicture)
        }
        size += 1
    }
    open fun addPictures(newPictures: List<Picture>) {
        _pictures.addAll(newPictures)
        size += newPictures.size
    }

    open fun removePicture(picture: Picture) {
        _pictures.remove(picture)
        size -= 1
    }

    private fun setCoverUri() {
        val uri = getOnePictureUri()
        if (uri != null) {
            cover["uri"] = uri
        } else {
            cover.remove("uri")
        }
    }

    abstract fun getOnePictureUri() : Uri?

}
