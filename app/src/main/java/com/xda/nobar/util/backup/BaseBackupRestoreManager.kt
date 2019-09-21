package com.xda.nobar.util.backup

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xda.nobar.R
import com.xda.nobar.util.prefManager
import com.xda.nobar.util.restartApp
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.*

abstract class BaseBackupRestoreManager(context: Context) : ContextWrapper(context) {
    internal abstract val type: String
    internal abstract val name: String

    internal val dateFormat = SimpleDateFormat("yyyy_MM_dd-HH_mm_ss", Locale.getDefault())

    val saveIntent: Intent
        get() = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "application/vnd.nobar.${this@BaseBackupRestoreManager.type}"
            putExtra(Intent.EXTRA_TITLE,
                    "NoBar_$name-${dateFormat.format(Date())}.nobarbak")
        }

    val openIntent: Intent
        get() = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/vnd.nobar.${this@BaseBackupRestoreManager.type}"

            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                    "application/nobarbak",
                    "application/octet-stream",
                    "nobar/${this@BaseBackupRestoreManager.type}",
                    "application/vnd.nobar.${this@BaseBackupRestoreManager.type}"
            ))
        }

    abstract fun saveBackup(dest: Uri)

    open fun applyBackup(src: Uri) {
        val deserialize = deserialize(src)

        if (deserialize == null) toastInvalid()
        else {
            deserialize.forEach {
                prefManager.put(it.key, it.value)
            }
            restored()
        }
    }

    internal fun serialize(dest: Uri, data: HashMap<String, Any?>) {
        contentResolver.openFileDescriptor(dest, "w")?.use { fd ->
            FileOutputStream(fd.fileDescriptor).use { out ->
                ObjectOutputStream(out).use { oos ->
                    oos.writeObject(data)
                }
            }
        }
    }

    internal fun deserialize(src: Uri): HashMap<String, Any?>? {
        try {
            contentResolver.openFileDescriptor(src, "r")?.use { fd ->
                FileInputStream(fd.fileDescriptor).use { input ->
                    ObjectInputStream(input).use { ois ->
                        return ois.readObject() as HashMap<String, Any?>
                    }
                }
            }
        } catch (e: Exception) {}

        return null
    }

    private fun toastInvalid() {
        Toast.makeText(this, R.string.invalid_backup, Toast.LENGTH_SHORT).show()
    }

    private fun restored() {
        Toast.makeText(this, R.string.restored, Toast.LENGTH_SHORT).show()

        MaterialAlertDialogBuilder(this)
                .setTitle(R.string.restart_app)
                .setMessage(R.string.restart_for_restore_desc)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    restartApp()
                }
                .setNegativeButton(android.R.string.no, null)
                .setCancelable(false)
                .show()
    }
}