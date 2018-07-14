package com.xda.nobar.tasker.activities

import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperNoOutputOrInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.xda.nobar.R
import com.xda.nobar.tasker.runners.ToggleGestureRunner

class ToggleGestureActivity : BaseToggleActivity() {
    override val helper: TaskerPluginConfigHelperNoOutputOrInput<ToggleGestureRunner> by lazy {
        object : TaskerPluginConfigHelperNoOutputOrInput<ToggleGestureRunner>(this) {
            override val runnerClass = ToggleGestureRunner::class.java
            override fun addToStringBlurb(input: TaskerInput<Unit>, blurbBuilder: StringBuilder) {
                blurbBuilder.append(resources.getString(R.string.toggle_gestures_desc))
            }
        }
    }
}