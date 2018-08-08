package com.xda.nobar.activities

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.xda.nobar.R
import com.xda.nobar.adapters.IntentSelectorAdapter
import com.xda.nobar.interfaces.OnIntentSelectedListener
import com.xda.nobar.util.IntentInfo
import com.xda.nobar.util.Utils

class IntentSelectorActivity : BaseAppSelectActivity<Int, IntentInfo>() {
    companion object {
        val INTENTS = hashMapOf(
                Pair(R.string.google_music_search,
                        Intent("com.google.android.googlequicksearchbox.MUSIC_SEARCH")),
                Pair(R.string.google_weather,
                        Intent("com.google.android.apps.gsa.velour.DynamicActivityTrampoline").apply {
                            data = Uri.parse("dynact://velour/weather/ProxyActivity")
                            `package` = "com.google.android.googlequicksearchbox"
                            component = ComponentName(
                                    `package`,
                                    "com.google.android.apps.gsa.velour.DynamicActivityTrampoline"
                            )
                        }
                ),
                Pair(R.string.google_new_calendar_event,
                        Intent(Intent.ACTION_EDIT).apply {
                            `package` = "com.google.android.calendar"
                            addCategory(Intent.CATEGORY_DEFAULT)
                            type = "vnd.android.cursor.item/event"
                        }
                ),
                Pair(R.string.camera_take_photo,
                        Intent(MediaStore.ACTION_IMAGE_CAPTURE)),
                Pair(R.string.camera_take_video,
                        Intent(MediaStore.ACTION_VIDEO_CAPTURE)),
                Pair(R.string.shazam_it,
                        Intent("com.shazam.android.intent.actions.START_TAGGING").apply {
                            addCategory(Intent.CATEGORY_DEFAULT)
                        }
                ),
                Pair(R.string.soundhound_it,
                        Intent("com.soundhound.android.ID_NOW_EXTERNAL").apply {
                            addCategory(Intent.CATEGORY_DEFAULT)
                        }
                )
        )
    }

    override val adapter by lazy {
        IntentSelectorAdapter(OnIntentSelectedListener {
            Utils.saveIntentKey(this, it.id, getKey())
            val resultIntent = Intent()
            resultIntent.putExtra(EXTRA_KEY, intent.getStringExtra(EXTRA_KEY))
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }, this)
    }

    override fun canRun() = intent != null && intent.hasExtra(EXTRA_KEY)

    override fun loadAppList(): ArrayList<Int> {
        return ArrayList(INTENTS.keys)
    }

    override fun loadAppInfo(info: Int): IntentInfo? {
        return IntentInfo(
                info,
                Utils.getIntentKey(this, getKey()) == info
        )
    }

    override fun filter(query: String): java.util.ArrayList<IntentInfo> {
        return ArrayList(ArrayList(origAppSet).filter { resources.getString(it.id).toLowerCase().contains(query.toLowerCase()) })
    }
}