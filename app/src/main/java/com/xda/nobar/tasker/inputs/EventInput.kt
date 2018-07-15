package com.xda.nobar.tasker.inputs

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
open class EventInput(@field:TaskerInputField("gesture") var gesture: String? = null)