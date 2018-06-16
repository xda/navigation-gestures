package com.xda.nobar.activities

import android.content.Context
import android.support.v7.app.AppCompatActivity
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput

open class TaskerConfig : AppCompatActivity(), TaskerPluginConfig<TaskerConfig.Input> {
    override val context: Context
        get() = this
    override val inputForTasker: TaskerInput<Input>
        get() = TaskerInput(Input())

    private var input: Input? = null

    override fun assignFromInput(input: TaskerInput<Input>) {
        this.input = input.regular
    }

    class Input {
        companion object {
            const val TOGGLE_NAV = 0
            const val TOGGLE_GEST = 1
        }

        var which = TOGGLE_GEST
    }
}
