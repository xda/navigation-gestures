package com.xda.nobar.tasker.activities

import android.os.Bundle
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.xda.nobar.activities.BaseAppSelectActivity
import com.xda.nobar.interfaces.OnAppSelectedListener
import com.xda.nobar.tasker.inputs.EventInput
import com.xda.nobar.tasker.runners.EventRunner
import com.xda.nobar.util.AppInfo
import com.xda.nobar.util.AppSelectAdapter

class EventConfigureActivity : BaseAppSelectActivity(), TaskerPluginConfig<EventInput> {
    override val context by lazy { this }
    override val inputForTasker: TaskerInput<EventInput>
        get() = TaskerInput(EventInput(gesture))
    override val adapter = AppSelectAdapter(true, false, OnAppSelectedListener {
        gesture = it.packageName
    }, false, false)

    private var gesture: String? = null

    private val helper: TaskerPluginConfigHelper<EventInput, Unit, EventRunner> by lazy {
        object : TaskerPluginConfigHelper<EventInput, Unit, EventRunner>(this) {
            override val runnerClass = EventRunner::class.java
            override val inputClass = EventInput::class.java
            override val outputClass = Unit::class.java
        }
    }

    override fun assignFromInput(input: TaskerInput<EventInput>) {
        gesture = input.regular.gesture
        adapter.setSelectedByPackage(gesture ?: return)
    }

    override fun loadAppInfo(info: Any): AppInfo? {
        info as String

        return AppInfo(
                info,
                "",
                app.name(info)!!,
                app.icon(info),
                info == gesture
        )
    }

    override fun loadAppList(): ArrayList<*> {
        return app.actionsList
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        helper.onCreate()
    }

    override fun onBackPressed() {
        helper.finishForTasker()
    }
}