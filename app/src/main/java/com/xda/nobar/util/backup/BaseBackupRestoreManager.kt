package com.xda.nobar.util.backup

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import com.xda.nobar.util.prefManager
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
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/nobarbak", "application/octet-stream", "nobar/${this@BaseBackupRestoreManager.type}"))
        }

    abstract fun saveBackup(dest: Uri)

    open fun applyBackup(src: Uri) {
        deserialize(src)?.forEach {
            prefManager.put(it.key, it.value)
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
        contentResolver.openFileDescriptor(src, "r")?.use { fd ->
            FileInputStream(fd.fileDescriptor).use { input ->
                ObjectInputStream(input).use { ois ->
                    return try {
                        ois.readObject() as HashMap<String, Any?>
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }

        return null
    }
}