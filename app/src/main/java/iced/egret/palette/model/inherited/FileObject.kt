package iced.egret.palette.model.inherited

/**
 * An object that represents a File: folders, images, videos, etc.
 */
interface FileObject {

    var filePath: String
    var parent: FileObject?
    val parentFilePath: String
        get() = filePath.split("/").dropLast(1).joinToString("/")

    /**
     * Indicates if (based on some defined conditions) the FileObject can be safely purged.
     */
    val deletable: Boolean

}