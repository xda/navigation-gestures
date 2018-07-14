package com.xda.nobar.tasker.states

import android.content.Context
import com.joaomgcd.taskerpluginlibrary.condition.TaskerPluginRunnerConditionState
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultCondition
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionSatisfied
import com.xda.nobar.tasker.inputs.BaseConditionInput

class EventState : TaskerPluginRunnerConditionState<BaseConditionInput, Unit>() {
    var input: TaskerInput<BaseConditionInput>? = null
    override fun getSatisfiedCondition(context: Context, input: TaskerInput<BaseConditionInput>, update: Unit?): TaskerPluginResultCondition<Unit> {
        this.input = input
        return TaskerPluginResultConditionSatisfied(context)
    }
}