package com.xda.nobar.tasker.activities

import android.os.Bundle
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.xda.nobar.activities.BaseAppSelectActivity
import com.xda.nobar.interfaces.OnAppSelectedListener
import com.xda.nobar.tasker.inputs.BaseConditionInput
import com.xda.nobar.tasker.runners.BaseConditionRunner
import com.xda.nobar.util.AppInfo
import com.xda.nobar.util.AppSelectAdapter

class EventConfigureActivity : BaseAppSelectActivity(), TaskerPluginConfig<BaseConditionInput> {
    override val context by lazy { this }
    override val inputForTasker: TaskerInput<BaseConditionInput>
        get() = TaskerInput(BaseConditionInput(gesture))
    override val adapter = AppSelectAdapter(true, false, OnAppSelectedListener {
        gesture = it.packageName
    }, false, false)

    private var gesture: String? = null

    private val helper: TaskerPluginConfigHelper<BaseConditionInput, Unit, BaseConditionRunner> by lazy {
        object : TaskerPluginConfigHelper<BaseConditionInput, Unit, BaseConditionRunner>(this) {
            override val runnerClass = BaseConditionRunner::class.java
            override val inputClass = BaseConditionInput::class.java
            override val outputClass = Unit::class.java
        }
    }

    override fun assignFromInput(input: TaskerInput<BaseConditionInput>) {
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