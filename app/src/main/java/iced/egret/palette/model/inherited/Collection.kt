package iced.egret.palette.model.inherited

import android.net.Uri
import android.view.View
import com.bumptech.glide.Glide
import iced.egret.palette.R
import iced.egret.palette.flexible.viewholder.CoverViewHolder
import iced.egret.palette.model.Picture

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
abstract class Collection(override var name: String, path: String, hasSetCoverable: Boolean = false) : Coverable {

    override val terminal = false
    override val cover = mutableMapOf<String, Any>(
            "id" to R.drawable.default_collection_cover
    )
    var hasCustomCoverable = hasSetCoverable
        private set
    var path = path
        protected set

    protected abstract var _pictures: MutableList<Picture>  // internal
    abstract val pictures: List<Picture>  // external

    abstract val contentsMap: Map<String, List<Coverable>>
    abstract val totalSize: Int
    var size = 0
        protected set

    override val longBlurb: String
        get() = "$totalSize items"
    override val shortBlurb: String
        get() {
            when {
                totalSize < 1_000 -> return totalSize.toString()
                totalSize < 100_000 -> return "~${totalSize / 1_000}K"
                totalSize < 100_000_000 -> return "~${totalSize / 1_000_000}M"
            }
            return "\uD83D\uDCAF"  // you have too many
        }

    fun isEmpty(): Boolean {
        return size == 0
    }

    fun isNotEmpty(): Boolean {
        return !isEmpty()
    }

    override fun toString(): String {
        return "$name, $size, $totalSize"
    }

    override fun loadCoverInto(holder: CoverViewHolder) {
        if (holder.ivItem != null) {
            setCoverUri()
            val imageReference: Any? = if (cover["uri"] != null) {
                cover["uri"]
            } else {
                cover["id"]
            }

            val glide =
                    Glide.with(holder.itemView.context)
                            .load(imageReference)
                            .centerCrop()
                            .error(R.drawable.ic_broken_image_black_24dp)

            // Load image with signature if possible
            buildGlideImage(glide, holder.ivItem, imageReference)

        }
        if (holder.textContainer != null) {
            holder.textContainer.visibility = View.VISIBLE
        }
    }


    abstract fun getContents(): List<Coverable>
    abstract fun rename(name: String)

    fun findPictureByPath(path: String): Picture? {
        return _pictures.find { picture -> picture.filePath == path }
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
        newPictures.map { p -> addPicture(p) }
    }

    open fun removePicture(picture: Picture) {
        _pictures.remove(picture)
        size -= 1
    }

    private fun setCoverUri() {
        if (hasCustomCoverable) return

        val uri = getOnePictureUri()
        if (uri != null) {
            cover["uri"] = uri
        } else {
            cover.remove("uri")
        }
    }

    abstract fun getOnePictureUri(): Uri?

    fun addCustomCover(picture: Picture) {
        hasCustomCoverable = true
        cover["uri"] = picture.uri
    }

    fun removeCustomCover() {
        hasCustomCoverable = false
    }

}
