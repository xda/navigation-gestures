package com.xda.nobar.tasker.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutputOrInput
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperNoOutputOrInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput

abstract class BaseNoInputOutputActivity : AppCompatActivity(), TaskerPluginConfig<Unit> {
    override val context  by lazy { this }
    override val inputForTasker: TaskerInput<Unit> = TaskerInput(Unit)

    override fun assignFromInput(input: TaskerInput<Unit>) {}

    internal abstract val helper: TaskerPluginConfigHelperNoOutputOrInput<out TaskerPluginRunnerActionNoOutputOrInput>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        helper.onCreate()
        helper.finishForTasker()
    }
}