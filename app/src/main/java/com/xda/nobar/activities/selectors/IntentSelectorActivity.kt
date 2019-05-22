package com.xda.nobar.activities.selectors

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.xda.nobar.R
import com.xda.nobar.adapters.IntentSelectorAdapter
import com.xda.nobar.adapters.info.IntentInfo
import com.xda.nobar.interfaces.OnIntentSelectedListener
import com.xda.nobar.util.prefManager

class IntentSelectorActivity : BaseAppSelectActivity<IntentSelectorActivity.TypeIntent, IntentInfo>() {
    companion object {
        const val BROADCAST = "broadcast"
        const val SERVICE = "service"
        const val ACTIVITY = "activity"

        val INTENTS = hashMapOf(
                0 to TypeIntent(
                        "com.google.android.googlequicksearchbox.MUSIC_SEARCH",
                        ACTIVITY,
                        R.string.google_music_search,
                        0
                ),
                1 to TypeIntent(
                        "com.google.android.sortedApps.gsa.velour.DynamicActivityTrampoline",
                        ACTIVITY,
                        R.string.google_weather,
                        1
                ).apply {
                    data = Uri.parse("dynact://velour/weather/ProxyActivity")
                    `package` = "com.google.android.googlequicksearchbox"
                    component = ComponentName(
                            `package`!!,
                            "com.google.android.sortedApps.gsa.velour.DynamicActivityTrampoline"
                    )
                },
                2 to TypeIntent(
                        Intent.ACTION_EDIT,
                        ACTIVITY,
                        R.string.google_new_calendar_event,
                        2
                ).apply {
                    `package` = "com.google.android.calendar"
                    addCategory(Intent.CATEGORY_DEFAULT)
                    type = "vnd.android.cursor.item/event"
                },
                3 to TypeIntent(
                        MediaStore.ACTION_IMAGE_CAPTURE,
                        ACTIVITY,
                        R.string.camera_take_photo,
                        3
                ),
                4 to TypeIntent(
                        MediaStore.ACTION_VIDEO_CAPTURE,
                        ACTIVITY,
                        R.string.camera_take_video,
                        4
                ),
                5 to TypeIntent(
                        "com.shazam.android.intent.actions.START_TAGGING",
                        ACTIVITY,
                        R.string.shazam_it,
                        5
                ).apply {
                    addCategory(Intent.CATEGORY_DEFAULT)
                },
                6 to TypeIntent(
                        "com.soundhound.android.ID_NOW_EXTERNAL",
                        ACTIVITY,
                        R.string.soundhound_it,
                        6
                ).apply {
                    addCategory(Intent.CATEGORY_DEFAULT)
                },
                7 to TypeIntent(
                        "com.bubblesoft.android.bubbleupnp.START_SERVICE",
                        BROADCAST,
                        R.string.start_bubble_upnp,
                        7
                ),
                8 to TypeIntent(
                        "com.bubblesoft.android.bubbleupnp.STOP_SERVICE",
                        BROADCAST,
                        R.string.stop_bubble_upnp,
                        8
                ),
                9 to TypeIntent(
                        SERVICE,
                        R.string.update_twilight,
                        9
                ).apply {
                    `package` = "com.urbanandroid.lux"
                    component = ComponentName(`package`!!, "com.urbandroid.lux.TwilightService")
                    putExtra("update", "Update")
                },
                10 to TypeIntent(
                        SERVICE,
                        R.string.refresh_twilight,
                        10
                ).apply {
                    `package` = "com.urbanandroid.lux"
                    component = ComponentName(`package`!!, "com.urbanandroid.lux.TwilightService")
                    putExtra("refresh", "Refresh")
                },
                11 to TypeIntent(
                        SERVICE,
                        R.string.next_twilight_profile,
                        11
                ).apply {
                    `package` = "com.urbanandroid.lux"
                    component = ComponentName(`package`!!, "com.urbanandroid.lux.TwilightService")
                    putExtra("profile_next", "Next Profile")
                },
                12 to TypeIntent(
                        SERVICE,
                        R.string.toggle_twilight,
                        12
                ).apply {
                    `package` = "com.urbanandroid.lux"
                    component = ComponentName(`package`!!, "com.urbanandroid.lux.TwilightService")
                    putExtra("toggle", "Toggle")
                },
                13 to TypeIntent(
                        SERVICE,
                        R.string.start_twilight,
                        13
                ).apply {
                    `package` = "com.urbanandroid.lux"
                    component = ComponentName(`package`!!, "com.urbanandroid.lux.TwilightService")
                    putExtra("start", "Start")
                },
                14 to TypeIntent(
                        SERVICE,
                        R.string.stop_twilight,
                        14
                ).apply {
                    `package` = "com.urbanandroid.lux"
                    component = ComponentName(`package`!!, "com.urbanandroid.lux.TwilightService")
                    putExtra("stop", "Stop")
                }
        )
    }

    override val adapter by lazy {
        IntentSelectorAdapter(OnIntentSelectedListener {
            prefManager.saveIntentKey(key, it.id)
            val resultIntent = Intent()
            resultIntent.putExtras(intent)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }, this)
    }

    override fun canRun() = intent != null && key != null

    override fun loadAppList(): ArrayList<TypeIntent> {
        return ArrayList(INTENTS.values)
    }

    override fun loadAppInfo(info: TypeIntent): IntentInfo? {
        return IntentInfo(
                info.id,
                info.res,
                prefManager.getIntentKey(key!!) == info.id
        )
    }

    override fun filter(query: String): java.util.ArrayList<IntentInfo> {
        return ArrayList(ArrayList(origAppSet).filter { resources.getString(it.res).toLowerCase().contains(query.toLowerCase()) })
    }

    class TypeIntent : Intent {
        var which: String? = null
        val res: Int
        val id: Int

        constructor(which: String, res: Int, id: Int) : super() {
            this.which = which
            this.res = res
            this.id = id
        }

        constructor(action: String, which: String, res: Int, id: Int) : super(action) {
            this.which = which
            this.res = res
            this.id = id
        }
    }
}