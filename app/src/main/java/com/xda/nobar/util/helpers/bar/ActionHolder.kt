package com.xda.nobar.util.helpers.bar

import android.content.Context
import com.xda.nobar.R
import com.xda.nobar.util.actionManager

class ActionHolder private constructor(private val context: Context) {
    companion object {
        private var instance: ActionHolder? = null

        fun getInstance(context: Context): ActionHolder {
            if (instance == null) instance = ActionHolder(context.applicationContext)

            return instance!!
        }
    }

    /**
     * Actions and Types
     * *********************************************************
     */
    val actionLeft: String by lazy { context.resources.getString(R.string.action_left) }
    val actionRight: String by lazy { context.resources.getString(R.string.action_right) }
    val actionUp: String by lazy { context.resources.getString(R.string.action_up) }
    val actionDown: String by lazy { context.resources.getString(R.string.action_down) }
    val actionDouble: String by lazy { context.resources.getString(R.string.action_double) }
    val actionHold: String by lazy { context.resources.getString(R.string.action_hold) }
    val actionTap: String by lazy { context.resources.getString(R.string.action_tap) }
    val actionUpHold: String by lazy { context.resources.getString(R.string.action_up_hold) }
    val actionLeftHold: String by lazy { context.resources.getString(R.string.action_left_hold) }
    val actionRightHold: String by lazy { context.resources.getString(R.string.action_right_hold) }
    val actionDownHold: String by lazy { context.resources.getString(R.string.action_down_hold) }

    val actionUpLeft: String by lazy { context.resources.getString(R.string.action_up_left) }
    val actionUpHoldLeft: String by lazy { context.resources.getString(R.string.action_up_hold_left) }

    val actionUpCenter: String by lazy { context.resources.getString(R.string.action_up_center) }
    val actionUpHoldCenter: String by lazy { context.resources.getString(R.string.action_up_hold_center) }

    val actionUpRight: String by lazy { context.resources.getString(R.string.action_up_right) }
    val actionUpHoldRight: String by lazy { context.resources.getString(R.string.action_up_hold_right) }

    val complexActionLeftUp: String by lazy { context.resources.getString(R.string.action_complex_left_up) }
    val complexActionRightUp: String by lazy { context.resources.getString(R.string.action_complex_right_up) }
    val complexActionLeftDown: String by lazy { context.resources.getString(R.string.action_complex_left_down) }
    val complexActionRightDown: String by lazy { context.resources.getString(R.string.action_complex_right_down) }
    val complexActionLongLeftUp: String by lazy { context.resources.getString(R.string.action_complex_long_left_up) }
    val complexActionLongRightUp: String by lazy { context.resources.getString(R.string.action_complex_long_right_up) }
    val complexActionLongLeftDown: String by lazy { context.resources.getString(R.string.action_complex_long_left_down) }
    val complexActionLongRightDown: String by lazy { context.resources.getString(R.string.action_complex_long_right_down) }

    val sideLeftIn: String by lazy { context.resources.getString(R.string.action_side_left_in) }
    val sideRightIn: String by lazy { context.resources.getString(R.string.action_side_right_in) }
    val sideLeftInLong: String by lazy { context.resources.getString(R.string.action_side_left_in_long) }
    val sideRightInLong: String by lazy { context.resources.getString(R.string.action_side_right_in_long) }

    val actionsList by lazy {
        arrayListOf(
            actionLeft,
            actionRight,
            actionUp,
            actionDown,
            actionDouble,
            actionHold,
            actionTap,
            actionUpHold,
            actionLeftHold,
            actionRightHold,
            actionDownHold,
            actionUpLeft,
            actionUpHoldLeft,
            actionUpCenter,
            actionUpHoldCenter,
            actionUpRight,
            actionUpHoldRight,
            complexActionLeftUp,
            complexActionRightUp,
            complexActionLeftDown,
            complexActionRightDown,
            complexActionLongLeftUp,
            complexActionLongRightUp,
            complexActionLongLeftDown,
            complexActionLongRightDown,
            sideLeftIn,
            sideRightIn,
            sideLeftInLong,
            sideRightInLong
        )
    }

    fun name(action: String): String? {
        val res = when (action) {
            actionLeft -> R.string.left
            actionRight -> R.string.right
            actionUp -> R.string.up
            actionDown -> R.string.down
            actionDouble -> R.string.double_tap
            actionHold -> R.string.hold
            actionTap -> R.string.tap
            actionUpHold -> R.string.swipe_up_hold
            actionLeftHold -> R.string.left_hold
            actionRightHold -> R.string.right_hold
            actionDownHold -> R.string.down_hold
            actionUpLeft -> R.string.swipe_up_left
            actionUpHoldLeft -> R.string.swipe_up_hold_left
            actionUpCenter -> R.string.swipe_up_center
            actionUpHoldCenter -> R.string.swipe_up_hold_center
            actionUpRight -> R.string.swipe_up_right
            actionUpHoldRight -> R.string.swipe_up_hold_right
            complexActionLeftUp -> R.string.swipe_left_up
            complexActionRightUp -> R.string.swipe_right_up
            complexActionLeftDown -> R.string.swipe_left_down
            complexActionRightDown -> R.string.swipe_right_down
            complexActionLongLeftUp -> R.string.swipe_long_left_up
            complexActionLongRightUp -> R.string.swipe_long_right_up
            complexActionLongLeftDown -> R.string.swipe_long_left_down
            complexActionLongRightDown -> R.string.swipe_long_right_down
            sideLeftIn -> R.string.swipe_in_from_left
            sideRightIn -> R.string.swipe_in_from_right
            sideLeftInLong -> R.string.long_swipe_in_from_left
            sideRightInLong -> R.string.long_swipe_in_from_right
            else -> 0
        }
        return if (res != 0) context.resources.getString(res)
        else null
    }

    fun icon(action: String): Int {
        return when (action) {
            actionLeft -> R.drawable.swipe_left
            actionRight -> R.drawable.swipe_right
            actionUp -> R.drawable.swipe_up
            actionDown -> R.drawable.swipe_down
            actionDouble -> R.drawable.double_tap
            actionHold -> R.drawable.tap_hold
            actionTap -> R.drawable.tap
            actionUpHold -> R.drawable.swipe_up_hold
            actionLeftHold -> R.drawable.swipe_left
            actionRightHold -> R.drawable.swipe_right
            actionDownHold -> R.drawable.swipe_down
            actionUpLeft -> R.drawable.swipe_up
            actionUpHoldLeft -> R.drawable.swipe_up_hold
            actionUpCenter -> R.drawable.swipe_up
            actionUpHoldCenter -> R.drawable.swipe_up_hold
            actionUpRight -> R.drawable.swipe_up
            actionUpHoldRight -> R.drawable.swipe_up_hold
            complexActionLeftUp -> R.drawable.ic_complex_up_left
            complexActionRightUp -> R.drawable.ic_complex_up_right
            complexActionLeftDown -> R.drawable.ic_complex_down_left
            complexActionRightDown -> R.drawable.ic_complex_down_right
            complexActionLongLeftUp -> R.drawable.ic_complex_up_left_long
            complexActionLongRightUp -> R.drawable.ic_complex_up_right_long
            complexActionLongLeftDown -> R.drawable.ic_complex_down_left_long
            complexActionLongRightDown -> R.drawable.ic_complex_down_right_long
            //TODO add icons for side swipes
            else -> 0
        }
    }

    fun hasAnyOfActions(vararg gestures: String): Boolean {
        val map = context.actionManager.actionMap

        gestures.forEach {
            if (map[it] != typeNoAction) return true
        }

        return false
    }

    fun hasAllOfActions(vararg gestures: String): Boolean {
        val map = context.actionManager.actionMap

        gestures.forEach {
            if (map[it].run { this == null || this == typeNoAction }) return false
        }

        return true
    }

    fun hasSomeUpAction() =
        hasAnyOfActions(
            actionUp,
            actionUpHold,
            actionUpLeft,
            actionUpHoldLeft,
            actionUpCenter,
            actionUpHoldCenter,
            actionUpRight,
            actionUpHoldRight
        )

    fun hasSomeUpHoldAction() =
        hasAnyOfActions(
            actionUpHold,
            actionUpHoldLeft,
            actionUpHoldCenter,
            actionUpHoldRight
        )

    val typeNoAction by lazy { context.resources.getString(R.string.type_no_action).toInt() }
    val typeBack by lazy { context.resources.getString(R.string.type_back).toInt() }
    val typeOhm by lazy { context.resources.getString(R.string.type_ohm).toInt() }
    val typeRecents by lazy { context.resources.getString(R.string.type_recents).toInt() }
    val typeHide by lazy { context.resources.getString(R.string.type_hide).toInt() }
    val typeSwitch by lazy { context.resources.getString(R.string.type_switch).toInt() }
    val typeAssist by lazy { context.resources.getString(R.string.type_assist).toInt() }
    val typeHome by lazy { context.resources.getString(R.string.type_home).toInt() }
    val premTypeNotif by lazy { context.resources.getString(R.string.prem_type_notif).toInt() }
    val premTypeQs by lazy { context.resources.getString(R.string.prem_type_qs).toInt() }
    val premTypePower by lazy { context.resources.getString(R.string.prem_type_power).toInt() }
    val typeSplit by lazy { context.resources.getString(R.string.type_split).toInt() }
    val premTypeCycleRinger by lazy {
        context.resources.getString(R.string.prem_type_cycle_ringer).toInt()
    }
    val premTypeToggleAutoBrightness by lazy {
        context.resources.getString(R.string.prem_type_toggle_auto_brightness).toInt()
    }
    val premTypeMute by lazy { context.resources.getString(R.string.prem_type_mute).toInt() }
    val premTypePlayPause by lazy {
        context.resources.getString(R.string.prem_type_play_pause).toInt()
    }
    val premTypePrev by lazy { context.resources.getString(R.string.prem_type_prev).toInt() }
    val premTypeNext by lazy { context.resources.getString(R.string.prem_type_next).toInt() }
    val premTypeSwitchIme by lazy {
        context.resources.getString(R.string.prem_type_switch_ime).toInt()
    }
    val premTypeLaunchApp by lazy {
        context.resources.getString(R.string.prem_type_launch_app).toInt()
    }
    val premTypeLockScreen by lazy {
        context.resources.getString(R.string.prem_type_lock_screen).toInt()
    }
    val premTypeScreenshot by lazy {
        context.resources.getString(R.string.prem_type_screenshot).toInt()
    }
    val premTypeLaunchActivity by lazy {
        context.resources.getString(R.string.prem_type_launch_activity).toInt()
    }
    val premTypeRot by lazy { context.resources.getString(R.string.prem_type_rot).toInt() }
    val premTypeTaskerEvent by lazy {
        context.resources.getString(R.string.prem_type_tasker_event).toInt()
    }
    val typeToggleNav by lazy { context.resources.getString(R.string.type_toggle_nav).toInt() }
    val premTypeFlashlight by lazy {
        context.resources.getString(R.string.prem_type_flashlight).toInt()
    }
    val premTypeVolumePanel by lazy {
        context.resources.getString(R.string.prem_type_volume_panel).toInt()
    }
    val premTypeBluetooth by lazy {
        context.resources.getString(R.string.prem_type_bluetooth).toInt()
    }
    val premTypeWiFi by lazy { context.resources.getString(R.string.prem_type_wifi).toInt() }
    val premTypeIntent by lazy { context.resources.getString(R.string.prem_type_intent).toInt() }
    val premTypeBatterySaver by lazy {
        context.resources.getString(R.string.prem_type_battery_saver).toInt()
    }
    val premTypeScreenTimeout by lazy {
        context.resources.getString(R.string.prem_type_screen_timeout).toInt()
    }
    val premTypeLaunchShortcut by lazy {
        context.resources.getString(R.string.prem_type_launch_shortcut).toInt()
    }
    val premTypeKillBackground by lazy {
        context.resources.getString(R.string.prem_type_kill_background).toInt()
    }
    val premTypeVolumeDown by lazy {
        context.resources.getString(R.string.prem_type_vol_down).toInt()
    }
    val premTypeVolumeUp by lazy { context.resources.getString(R.string.prem_type_vol_up).toInt() }
    val premTypeBrightnessDown by lazy {
        context.resources.getString(R.string.prem_type_brightness_down).toInt()
    }
    val premTypeBrightnessUp by lazy {
        context.resources.getString(R.string.prem_type_brightness_up).toInt()
    }
    val premTypeAppDrawer by lazy {
        context.resources.getString(R.string.prem_type_app_drawer).toInt()
    }
    val premTypeToggleRotationLock by lazy {
        context.resources.getString(R.string.prem_type_toggle_rotation_lock).toInt()
    }
    val typeRootAccessibilityMenu by lazy {
        context.resources.getString(R.string.type_accessibility_button).toInt()
    }
    val typeRootChooseAccessibilityMenu by lazy {
        context.resources.getString(R.string.type_choose_accessibility_button).toInt()
    }

    val typeRootHoldBack by lazy { context.resources.getString(R.string.type_hold_back).toInt() }
    val typeRootForward by lazy { context.resources.getString(R.string.type_forward).toInt() }
    val typeRootMenu by lazy { context.resources.getString(R.string.type_menu).toInt() }
    val typeRootKillCurrentApp by lazy {
        context.resources.getString(R.string.root_type_kill_current_app).toInt()
    }
    val typeRootKeycode by lazy {
        context.resources.getString(R.string.root_type_send_keycode).toInt()
    }
    val typeRootDoubleKeycode by lazy {
        context.resources.getString(R.string.root_type_send_double_keycode).toInt()
    }
    val typeRootLongKeycode by lazy {
        context.resources.getString(R.string.root_type_send_long_keycode).toInt()
    }
}