package com.xda.nobar.tasker.activities

import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperNoOutputOrInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.xda.nobar.R
import com.xda.nobar.tasker.runners.ToggleNavRunner

class ToggleNavActivity : BaseNoInputOutputActivity() {
    override val helper: TaskerPluginConfigHelperNoOutputOrInput<ToggleNavRunner> by lazy {
        object : TaskerPluginConfigHelperNoOutputOrInput<ToggleNavRunner>(this) {
            override val runnerClass = ToggleNavRunner::class.java
            override fun addToStringBlurb(input: TaskerInput<Unit>, blurbBuilder: StringBuilder) {
                blurbBuilder.append(resources.getString(R.string.toggle_nav_desc))
            }
        }
    }
}