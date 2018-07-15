package com.xda.nobar.tasker.runners

import android.content.Context
import com.joaomgcd.taskerpluginlibrary.condition.TaskerPluginRunnerConditionEvent
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultCondition
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionSatisfied
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionUnsatisfied
import com.xda.nobar.tasker.inputs.BaseConditionInput
import com.xda.nobar.tasker.states.EventUpdate

class BaseConditionRunner : TaskerPluginRunnerConditionEvent<BaseConditionInput, Unit, EventUpdate>() {
    override fun getSatisfiedCondition(context: Context, input: TaskerInput<BaseConditionInput>, update: EventUpdate?): TaskerPluginResultCondition<Unit> {
        return if (input.regular.gesture == update?.gesture) TaskerPluginResultConditionSatisfied(context)
        else TaskerPluginResultConditionUnsatisfied()
    }
}