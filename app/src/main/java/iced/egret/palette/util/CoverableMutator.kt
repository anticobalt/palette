package iced.egret.palette.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import iced.egret.palette.R
import iced.egret.palette.model.Album
import iced.egret.palette.model.Folder
import iced.egret.palette.model.Picture
import iced.egret.palette.model.inherited.Collection
import iced.egret.palette.model.inherited.Coverable
import iced.egret.palette.model.inherited.FileObject
import java.io.File

/**
 * Handles changes to Coverables. Different activities use the same functions to do tasks
 * in response to user action under different contexts.
 */
object CoverableMutator {

    fun rename(album: Album, context: Context, onFinish: () -> Unit) {
        fun nameInUse(name: CharSequence): Boolean {
            val parent = album.parent
            if (parent != null) {
                return parent.albums.find { a ->
                    a.name == name.toString() && a != album
                } != null
            } else {
                return CollectionManager.albums.find { a ->
                    a.name == name.toString() && a != album
                } != null
            }
        }
        DialogGenerator.renameAlbum(context, album.name, ::nameInUse) {
            CollectionManager.renameCollection(album, it.toString())
            onFinish()
        }
    }

    fun rename(picture: Picture, context: Context, onFinish: () -> Unit) {
        val nameList = picture.name.split(".")
        val nameWithoutExtension = nameList.dropLast(1).joinToString(".")
        val extension = nameList.last()

        DialogGenerator.nameFile(context, nameWithoutExtension) { charSequence, dialog ->
            val newName = "$charSequence.$extension"
            if (Storage.fileExists(newName, picture.parentFilePath)) {
                toast(context, R.string.already_exists_error)
            } else {
                val files = CollectionManager.renamePicture(picture, newName, getSdCardDocumentFile(context))
                if (files == null) {
                    toastLong(context, context.getString(R.string.edit_fail_error) + " " +
                            context.getString(R.string.storage_fail_error_explain))
                } else {
                    // File by old name is technically it's own file
                    broadcastMediaChanged(context, files.first)
                    broadcastMediaChanged(context, files.second)
                    toast(context, R.string.file_save_success)
                    dialog.dismiss()  // nameFile dialog has no auto-dismiss, so do it manually
                    onFinish()
                }
            }
        }
    }

    fun createTopAlbum(context: Context, onFinish: () -> Unit) {
        fun albumExists(name: CharSequence): Boolean {
            val found = CollectionManager.albums.find { album -> album.name == name.toString() }
            return found != null
        }
        DialogGenerator.createAlbum(context, ::albumExists) {
            val name = it
            CollectionManager.createNewAlbum(name.toString())
            onFinish()
        }
    }

    fun deleteTopAlbums(albums: List<Album>, context: Context, onFinish: () -> Unit) {
        DialogGenerator.deleteAlbum(context) {
            CollectionManager.deleteAlbums(albums, fromCurrent = false)
            onFinish()
        }
    }

    fun createNestedAlbum(siblings: List<Coverable>, context: Context, onFinish: () -> Unit) {
        fun albumExists(name: CharSequence): Boolean {
            val found = siblings.find { coverable ->
                coverable is Album
                        && coverable.name == name.toString()
            }
            return found != null
        }
        DialogGenerator.createAlbum(context, ::albumExists) {
            CollectionManager.createNewAlbum(it.toString(), addToCurrent = true)
            onFinish()
        }
    }

    fun syncToAlbum(folders: List<Folder>, context: Context, onFinish: () -> Unit) {

        val albums = CollectionManager.getNestedAlbums()

        // If only one folder, should skip the albums that are already synced.
        // TODO: skip all albums in common
        val toSkip = if (folders.size == 1) {
            albums.filter { album -> folders[0].path in album.syncedFolderFiles.map { file -> file.path } }
                    .map { album -> albums.indexOf(album) }
                    .toIntArray()
        } else intArrayOf()

        DialogGenerator.syncToAlbum(context, albums, toSkip) {
            val skipSet = toSkip.toSet()
            val toSyncTo = it.filter { i -> i !in skipSet }.map { i -> albums[i] }

            for (album in toSyncTo) {
                val syncedPathSet = album.syncedFolderFiles.map { file -> file.path }.toMutableSet()
                for (folder in folders) {
                    syncedPathSet.add(folder.path)
                }
                CollectionManager.applySyncedFolders(syncedPathSet, false, album)
            }
            toast(context, R.string.success_sync)
            onFinish()
        }
    }

    fun addToAlbum(coverables: List<Coverable>, context: Context, onFinish: () -> Unit) {

        // If only one coverable, hide albums it is already in
        // TODO: hide albums shared by multiple coverables
        var albums = CollectionManager.getNestedAlbums().toList()
        if (albums.isEmpty()) {
            toast(context, R.string.no_albums_exist_error)
            return
        }
        if (coverables.size == 1) albums = albums.filterNot { album -> coverables[0] in album.pictures }
        if (albums.isEmpty()) {
            toast(context, R.string.no_albums_available_error)
            return
        }

        DialogGenerator.addToAlbum(context, albums) { indices ->
            val destinations = albums.filterIndexed { index, _ -> indices.contains(index) }
            CollectionManager.addContentToAllAlbums(coverables, destinations)
            toast(context, R.string.success_add_generic)
            onFinish()
        }
    }

    fun removeFromAlbum(coverables: List<Coverable>, context: Context, onFinish: () -> Unit) {
        DialogGenerator.removeFromAlbum(context) {
            CollectionManager.removeContentFromCurrentAlbum(coverables)
            onFinish()
        }
    }

    fun move(pictures: List<Picture>, context: Context, onFinish: () -> Unit) {

        // If all pictures are in same location, set it as initial
        val oldLocations = pictures.map { picture -> picture.parentFilePath }.toSet()
        val initialLocation = if (oldLocations.size == 1) oldLocations.min() else null

        DialogGenerator.moveTo(context, initialLocation) {
            if (initialLocation == it.path) {
                toast(context, R.string.already_exists_error)
                return@moveTo
            }
            val failedCounter = CollectionManager.movePictures(pictures, it,
                    getSdCardDocumentFile(context), context.contentResolver) { sourceFile, movedFile ->
                broadcastMediaChanged(context, sourceFile)
                broadcastMediaChanged(context, movedFile)
            }
            if (failedCounter > 0) {
                if (pictures.size > 1) {
                    toastLong(context, "Failed to move $failedCounter pictures! " +
                            context.getString(R.string.storage_fail_error_explain))
                } else {
                    toastLong(context, "Failed! " +
                            context.getString(R.string.storage_fail_error_explain))
                }
            } else toast(context, R.string.success_move_generic)
            onFinish()
        }
    }

    fun delete(pictures: List<Picture>, context: Context, onFinish: () -> Unit) {
        DialogGenerator.moveToRecycleBin(context) {
            val failedCounter = CollectionManager.movePicturesToRecycleBin(pictures, getSdCardDocumentFile(context)) {
                // If moved a Picture successfully, broadcast change
                broadcastMediaChanged(context, it)
            }
            if (failedCounter > 0) {
                if (pictures.size > 1) {
                    toastLong(context, "Failed to move $failedCounter pictures to recycle bin! " +
                            context.getString(R.string.storage_fail_error_explain))
                } else {
                    toastLong(context, "Failed! " +
                            context.getString(R.string.storage_fail_error_explain))
                }
            } else toast(context, R.string.success_move_to_recycle)
            onFinish()
        }
    }

    fun delete(albums: List<Album>, fromCurrent: Boolean, context: Context, onFinish: () -> Unit) {
        DialogGenerator.deleteAlbum(context) {
            CollectionManager.deleteAlbums(albums, fromCurrent)
            toast(context, R.string.success_delete_generic)
            onFinish()
        }
    }

    fun setAsCover(picture: Picture, context: Context, onFinish: () -> Unit) {
        val candidates = mutableSetOf<Collection>()

        // Get (most) Folders that this Picture is in, directly or indirectly
        var obj: FileObject? = picture.parent
        val pinnedFolders = CollectionManager.folders
        while (obj != null) {
            if (obj is Folder) candidates.add(obj)
            if (obj in pinnedFolders) break
            obj = obj.parent
        }
        // Get all Albums this Picture is directly in
        // TODO: handle indirection by making Albums doubly-linked
        for (album in CollectionManager.getNestedAlbums()) {
            if (picture in album.pictures) candidates.add(album)
        }
        // Get all pinned Albums
        for (album in CollectionManager.albums) {
            candidates.add(album)
        }

        val candidateCollections = candidates.sortedBy { c -> c.path }
        val candidateStrings = candidateCollections.map { c -> if (c in pinnedFolders) c.name else c.path }
        val alreadySetIndices = candidateCollections
                .filter { c -> c.hasCustomCoverable && c.cover["uri"] == picture.uri }
                .map { c -> candidateCollections.indexOf(c) }
                .toIntArray()

        DialogGenerator.setAsCover(candidateStrings, alreadySetIndices, context) {
            val skipSet = alreadySetIndices.toSet()
            val toCover = it.filter { int -> int !in skipSet }.map { i -> candidateCollections[i] }

            for (collection in toCover) {
                collection.addCustomCover(picture)
            }
            Storage.setCustomCovers(toCover.map { collection -> Pair(collection.path, picture.filePath) })

            onFinish()
        }
    }

    fun resetCover(collection: Collection, context: Context, onFinish: () -> Unit) {
        DialogGenerator.genericConfirm(context.getString(R.string.action_reset_cover), context) {
            collection.removeCustomCover()
            Storage.setCustomCover(collection.path, null)
            onFinish()
        }
    }

    private fun getSdCardDocumentFile(context: Context): DocumentFile? {
        val preferences = context.getSharedPreferences(
                context.getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
        )
        val uriAsString = preferences.getString(
                context.getString(R.string.sd_card_uri_key), null)
                ?: return null
        val uri = Uri.parse(uriAsString)
        return DocumentFile.fromTreeUri(context, uri)
    }

    /**
     * Broadcast changes so that they show up immediately whenever MediaStore is accessed.
     * https://stackoverflow.com/a/39241495
     */
    private fun broadcastMediaChanged(context: Context, file: File) {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val uri = Uri.fromFile(file)
        mediaScanIntent.data = uri
        context.sendBroadcast(mediaScanIntent)
    }

    private fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun toast(context: Context, res: Int) {
        Toast.makeText(context, res, Toast.LENGTH_SHORT).show()
    }

    private fun toastLong(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}