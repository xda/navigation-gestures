package com.xda.nobar.tasker.runners

import android.content.Context
import com.joaomgcd.taskerpluginlibrary.condition.TaskerPluginRunnerConditionEvent
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultCondition
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionSatisfied
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionUnsatisfied
import com.xda.nobar.tasker.inputs.BaseConditionInput
import com.xda.nobar.tasker.states.EventState

class BaseConditionRunner : TaskerPluginRunnerConditionEvent<BaseConditionInput, Unit, EventState>() {
    override fun getSatisfiedCondition(context: Context, input: TaskerInput<BaseConditionInput>, update: EventState?): TaskerPluginResultCondition<Unit> {
        return if (input.regular.gesture == update?.input?.regular?.gesture) TaskerPluginResultConditionSatisfied(context)
        else TaskerPluginResultConditionUnsatisfied()
    }
}