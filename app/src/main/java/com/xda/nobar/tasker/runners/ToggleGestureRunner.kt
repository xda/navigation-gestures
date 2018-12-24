package com.xda.nobar.tasker.runners

import android.content.Context
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutputOrInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import com.xda.nobar.R
import com.xda.nobar.util.Utils
import com.xda.nobar.util.app

class ToggleGestureRunner : TaskerPluginRunnerActionNoOutputOrInput() {
    override fun run(context: Context, input: TaskerInput<Unit>): TaskerPluginResult<Unit> {
        return if (Utils.runPremiumAction(context.app, context.app.isValidPremium) { context.app.toggleGestureBar() }) TaskerPluginResultSucess()
        else TaskerPluginResultError(SecurityException(context.resources.getString(R.string.premium_required)))
    }
}