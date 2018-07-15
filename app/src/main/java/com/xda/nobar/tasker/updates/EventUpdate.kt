package com.xda.nobar.tasker.updates

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
class EventUpdate(@field:TaskerInputField("gesture") var gesture: String? = null)