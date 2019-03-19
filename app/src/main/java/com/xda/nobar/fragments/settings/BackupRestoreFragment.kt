package com.xda.nobar.fragments.settings

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.preference.Preference
import com.xda.nobar.R
import com.xda.nobar.util.backup.AppearanceBackupRestoreManager
import com.xda.nobar.util.backup.BehaviorBackupRestoreManager
import com.xda.nobar.util.backup.GestureBackupRestoreManager
import com.xda.nobar.util.backup.OmniBackupRestoreManager

class BackupRestoreFragment : BasePrefFragment() {
    companion object {
        const val BACK_UP_GESTURES = "back_up_gestures"
        const val BACK_UP_APPEARANCE = "back_up_appearance"
        const val BACK_UP_BEHAVIOR = "back_up_behavior"
        const val BACK_UP_OMNI = "back_up_omni"

        const val RESTORE_GESTURES = "restore_gestures"
        const val RESTORE_APPEARANCE = "restore_appearance"
        const val RESTORE_BEHAVIOR = "restore_behavior"
        const val RESTORE_OMNI = "restore_omni"

        const val REQ_BACK_UP_GESTURES = 100
        const val REQ_BACK_UP_APPEARANCE = 102
        const val REQ_BACK_UP_BEHAVIOR = 104
        const val REQ_BACK_UP_OMNI = 106

        const val REQ_RESTORE_GESTURES = 101
        const val REQ_RESTORE_APPEARANCE = 103
        const val REQ_RESTORE_BEHAVIOR = 105
        const val REQ_RESTORE_OMNI = 107
    }

    override val resId = R.xml.prefs_backup_restore

    private val gesture by lazy { GestureBackupRestoreManager(context!!) }
    private val appearance by lazy { AppearanceBackupRestoreManager(context!!) }
    private val behavior by lazy { BehaviorBackupRestoreManager(context!!) }
    private val omni by lazy { OmniBackupRestoreManager(context!!) }

    override fun onResume() {
        super.onResume()

        activity?.setTitle(R.string.backup_and_restore)
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        return when(preference?.key) {
            BACK_UP_GESTURES -> {
                startActivityForResult(gesture.saveIntent, REQ_BACK_UP_GESTURES)
                true
            }
            BACK_UP_APPEARANCE -> {
                startActivityForResult(appearance.saveIntent, REQ_BACK_UP_APPEARANCE)
                true
            }
            BACK_UP_BEHAVIOR -> {
                startActivityForResult(behavior.saveIntent, REQ_BACK_UP_BEHAVIOR)
                true
            }
            BACK_UP_OMNI -> {
                startActivityForResult(omni.saveIntent, REQ_BACK_UP_OMNI)
                true
            }

            RESTORE_GESTURES -> {
                startActivityForResult(gesture.openIntent, REQ_RESTORE_GESTURES)
                true
            }
            RESTORE_APPEARANCE -> {
                startActivityForResult(appearance.openIntent, REQ_RESTORE_APPEARANCE)
                true
            }
            RESTORE_BEHAVIOR -> {
                startActivityForResult(behavior.openIntent, REQ_RESTORE_BEHAVIOR)
                true
            }
            RESTORE_OMNI -> {
                startActivityForResult(omni.openIntent, REQ_RESTORE_OMNI)
                true
            }

            else -> super.onPreferenceTreeClick(preference)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQ_BACK_UP_GESTURES -> {
                    gesture.saveBackup(data?.data ?: return)
                    saved()
                }
                REQ_BACK_UP_APPEARANCE -> {
                    appearance.saveBackup(data?.data ?: return)
                    saved()
                }
                REQ_BACK_UP_BEHAVIOR -> {
                    behavior.saveBackup(data?.data ?: return)
                    saved()
                }
                REQ_BACK_UP_OMNI -> {
                    omni.saveBackup(data?.data ?: return)
                    saved()
                }

                REQ_RESTORE_GESTURES -> {
                    gesture.applyBackup(data?.data ?: return)
                    restored()
                }
                REQ_RESTORE_APPEARANCE -> {
                    appearance.applyBackup(data?.data ?: return)
                    restored()
                }
                REQ_RESTORE_BEHAVIOR -> {
                    behavior.applyBackup(data?.data ?: return)
                    restored()
                }
                REQ_RESTORE_OMNI -> {
                    omni.applyBackup(data?.data ?: return)
                    restored()
                }
            }
        }
    }

    private fun saved() {
        Toast.makeText(context, R.string.saved, Toast.LENGTH_SHORT).show()
    }

    private fun restored() {
        Toast.makeText(context, R.string.restored, Toast.LENGTH_SHORT).show()
    }
}