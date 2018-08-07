package com.xda.nobar.activities

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import com.xda.nobar.R
import com.xda.nobar.interfaces.OnAppSelectedListener
import com.xda.nobar.util.AppInfo
import com.xda.nobar.util.AppSelectAdapter
import com.xda.nobar.util.Utils

class IntentSelectorActivity : BaseAppSelectActivity<Int>() {
    companion object {
        const val WHICH_INT = "checked_int"

        private fun makeIntent(
                action: String = "",
                data: Uri? = null,
                component: ComponentName? = null,
                category: String? = null,
                mimetype: String? = null,
                extras: Bundle? = null) =
                Intent(action).apply {
                    this.data = data
                    this.component = component
                    this.`package` = component?.packageName
                    this.categories.add(category)
                    this.type = mimetype
                    if (extras != null) putExtras(extras)
                }

        private val INTENTS = hashMapOf(
                Pair(R.string.google_music_search,
                        makeIntent("com.google.android.googlequicksearchbox.MUSIC_SEARCH")),
                Pair(R.string.google_weather,
                        makeIntent("com.google.android.apps.gsa.velour.DynamicActivityTrampoline",
                                Uri.parse("dynact://velour/weather/ProxyActivity"),
                                ComponentName(
                                        "com.google.android.googlequicksearchbox",
                                        "com.google.android.apps.gsa.velour.DynamicActivityTrampoline"
                                ))),
                Pair(R.string.google_new_calendar_event,
                        makeIntent("android.intent.action.EDIT",
                                null,
                                ComponentName(
                                        "com.google.android.calendar",
                                        null
                                ),
                                Intent.CATEGORY_DEFAULT,
                                "vnd.android.cursor.item/event")),
                Pair(R.string.camera_take_photo,
                        makeIntent(MediaStore.ACTION_IMAGE_CAPTURE)),
                Pair(R.string.camera_take_video,
                        makeIntent(MediaStore.ACTION_VIDEO_CAPTURE)),
                Pair(R.string.shazam_it,
                        makeIntent("com.shazam.android.intent.actions.START_TAGGING",
                                null, null,
                                Intent.CATEGORY_DEFAULT)),
                Pair(R.string.soundhound_it,
                        makeIntent("com.soundhound.android.ID_NOW_EXTERNAL",
                                null, null,
                                Intent.CATEGORY_DEFAULT))
        )
    }

    override val adapter: AppSelectAdapter = AppSelectAdapter(true, false, OnAppSelectedListener {

    })

    override fun canRun() = intent != null && intent.hasExtra(EXTRA_KEY)

    override fun loadAppList(): ArrayList<Int> {
        return ArrayList(INTENTS.keys)
    }

    override fun loadAppInfo(info: Int): AppInfo? {
        val intent = INTENTS[info] ?: return null
        return AppInfo(
                intent.`package`,
                info.toString(),
                resources.getString(info),
                0,
                Utils.getIntentKey(this, getKey()) == info
        )
    }
}