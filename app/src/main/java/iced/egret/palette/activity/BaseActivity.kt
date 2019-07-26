package iced.egret.palette.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import iced.egret.palette.R
import java.io.File

abstract class BaseActivity : AppCompatActivity() {

    fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun toast(resInt: Int) {
        Toast.makeText(this, resInt, Toast.LENGTH_SHORT).show()
    }

    fun getSdCardDocumentFile(): DocumentFile? {
        val preferences = getSharedPreferences(
                getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
        )
        val uriAsString = preferences
                .getString(getString(R.string.sd_card_uri_key), null) ?: return null
        val uri = Uri.parse(uriAsString)
        return DocumentFile.fromTreeUri(this, uri)
    }

    /**
     * Broadcast changes so that they show up immediately whenever MediaStore is accessed.
     * https://stackoverflow.com/a/39241495
     */
    fun broadcastMediaChanged(file: File) {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val uri = Uri.fromFile(file)
        mediaScanIntent.data = uri
        sendBroadcast(mediaScanIntent)
    }

}