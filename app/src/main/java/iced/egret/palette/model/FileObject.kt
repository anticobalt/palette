package iced.egret.palette.model

/**
 * An object that represents a File: folders, images, videos, etc.
 */
interface FileObject {

    var filePath: String
    var parent: FileObject?

    /**
     * Indicates if (based on some defined conditions) the FileObject can be safely purged.
     */
    val deletable: Boolean

}