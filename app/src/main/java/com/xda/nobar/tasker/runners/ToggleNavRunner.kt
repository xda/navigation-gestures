package com.xda.nobar.tasker.runners

import android.content.Context
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutputOrInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.util.Utils

class ToggleNavRunner : TaskerPluginRunnerActionNoOutputOrInput() {
    override fun run(context: Context, input: TaskerInput<Unit>): TaskerPluginResult<Unit> {
        val app = context.applicationContext as App
        return if (Utils.runPremiumAction(app) { app.toggleNavState() }) TaskerPluginResultSucess()
        else TaskerPluginResultError(SecurityException(app.resources.getString(R.string.premium_required)))
    }
}