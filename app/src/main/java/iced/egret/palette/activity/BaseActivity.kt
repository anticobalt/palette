package iced.egret.palette.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import iced.egret.palette.R
import java.io.File

abstract class BaseActivity : AppCompatActivity() {

    enum class ColorType { PRIMARY, ACCENT, ITEM }
    protected lateinit var defSharedPreferences : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        defSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    }

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

    fun idToColor(colorResId: Int) = ContextCompat.getColor(this, colorResId)

    fun getColorInt(type: ColorType) : Int {
        val keyRef = when (type) {
            ColorType.PRIMARY -> R.string.primary_color_key
            ColorType.ACCENT -> R.string.accent_color_key
            ColorType.ITEM -> R.string.toolbar_item_color_key
        }
        val defaultRef = when (type) {
            ColorType.PRIMARY -> R.color.cyanea_primary_reference
            ColorType.ACCENT -> R.color.cyanea_accent
            ColorType.ITEM -> R.color.white
        }
        return defSharedPreferences.getInt(getString(keyRef), idToColor(defaultRef))
    }

}