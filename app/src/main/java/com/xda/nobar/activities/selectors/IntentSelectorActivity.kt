package com.xda.nobar.activities.selectors

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.xda.nobar.R
import com.xda.nobar.adapters.IntentSelectorAdapter
import com.xda.nobar.interfaces.OnIntentSelectedListener
import com.xda.nobar.adapters.info.IntentInfo

class IntentSelectorActivity : BaseAppSelectActivity<Int, IntentInfo>() {
    companion object {
        const val BROADCAST = "broadcast"
        const val SERVICE = "service"
        const val ACTIVITY = "activity"

        val INTENTS = hashMapOf(
                R.string.google_music_search to
                        TypeIntent("com.google.android.googlequicksearchbox.MUSIC_SEARCH", ACTIVITY),
                R.string.google_weather to
                        TypeIntent("com.google.android.apps.gsa.velour.DynamicActivityTrampoline", ACTIVITY).apply {
                            data = Uri.parse("dynact://velour/weather/ProxyActivity")
                            `package` = "com.google.android.googlequicksearchbox"
                            component = ComponentName(
                                    `package`!!,
                                    "com.google.android.apps.gsa.velour.DynamicActivityTrampoline"
                            )
                        },
                R.string.google_new_calendar_event to
                        TypeIntent(Intent.ACTION_EDIT, ACTIVITY).apply {
                            `package` = "com.google.android.calendar"
                            addCategory(Intent.CATEGORY_DEFAULT)
                            type = "vnd.android.cursor.item/event"
                        },
                R.string.camera_take_photo to
                        TypeIntent(MediaStore.ACTION_IMAGE_CAPTURE, ACTIVITY),
                R.string.camera_take_video to
                        TypeIntent(MediaStore.ACTION_VIDEO_CAPTURE, ACTIVITY),
                R.string.shazam_it to
                        TypeIntent("com.shazam.android.intent.actions.START_TAGGING", ACTIVITY).apply {
                            addCategory(Intent.CATEGORY_DEFAULT)
                        },
                R.string.soundhound_it to
                        TypeIntent("com.soundhound.android.ID_NOW_EXTERNAL", ACTIVITY).apply {
                            addCategory(Intent.CATEGORY_DEFAULT)
                        },
                R.string.start_bubble_upnp to
                        TypeIntent("com.bubblesoft.android.bubbleupnp.START_SERVICE", BROADCAST),
                R.string.stop_bubble_upnp to
                        TypeIntent("com.bubblesoft.android.bubbleupnp.STOP_SERVICE", BROADCAST),
                R.string.update_twilight to
                        TypeIntent(SERVICE).apply {
                            `package` = "com.urbanandroid.lux"
                            component = ComponentName(`package`!!, "com.urbandroid.lux.TwilightService")
                            putExtra("update", "Update")
                        },
                R.string.refresh_twilight to
                        TypeIntent(SERVICE).apply {
                            `package` = "com.urbanandroid.lux"
                            component = ComponentName(`package`!!, "com.urbanandroid.lux.TwilightService")
                            putExtra("refresh", "Refresh")
                        },
                R.string.next_twilight_profile to
                        TypeIntent(SERVICE).apply {
                            `package` = "com.urbanandroid.lux"
                            component = ComponentName(`package`!!, "com.urbanandroid.lux.TwilightService")
                            putExtra("profile_next", "Next Profile")
                        },
                R.string.toggle_twilight to
                        TypeIntent(SERVICE).apply {
                            `package` = "com.urbanandroid.lux"
                            component = ComponentName(`package`!!, "com.urbanandroid.lux.TwilightService")
                            putExtra("toggle", "Toggle")
                        },
                R.string.start_twilight to
                        TypeIntent(SERVICE).apply {
                            `package` = "com.urbanandroid.lux"
                            component = ComponentName(`package`!!, "com.urbanandroid.lux.TwilightService")
                            putExtra("start", "Start")
                        },
                R.string.stop_twilight to
                        TypeIntent(SERVICE).apply {
                            `package` = "com.urbanandroid.lux"
                            component = ComponentName(`package`!!, "com.urbanandroid.lux.TwilightService")
                            putExtra("stop", "Stop")
                        }
        )
    }

    override val adapter by lazy {
        IntentSelectorAdapter(OnIntentSelectedListener {
            prefManager.saveIntentKey(getKey(), it.id)
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
                prefManager.getIntentKey(getKey()) == info
        )
    }

    override fun filter(query: String): java.util.ArrayList<IntentInfo> {
        return ArrayList(ArrayList(origAppSet).filter { resources.getString(it.id).toLowerCase().contains(query.toLowerCase()) })
    }

    class TypeIntent : Intent {
        var which: String? = null

        constructor(which: String) : super() {
            this.which = which
        }
        constructor(action: String, which: String) : super(action) {
            this.which = which
        }
    }
}