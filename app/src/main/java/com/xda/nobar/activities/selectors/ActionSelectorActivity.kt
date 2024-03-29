package com.xda.nobar.activities.selectors

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xda.nobar.IActionSelectedCallback
import com.xda.nobar.R
import com.xda.nobar.adapters.ActionSelectAdapter
import com.xda.nobar.adapters.info.ActionInfo
import com.xda.nobar.prefs.SectionableListPreference
import com.xda.nobar.util.actionHolder
import com.xda.nobar.util.prefManager
import kotlinx.android.synthetic.main.text_input.view.*
import java.util.*
import kotlin.collections.ArrayList

class ActionSelectorActivity : BaseAppSelectActivity<ActionInfo, ActionInfo>() {
    companion object {
        const val EXTRA_SECTIONS = "sections"
        const val EXTRA_BUNDLE = "bundle"
        const val EXTRA_CALLBACK = "callback"
        const val EXTRA_GESTURE = "gesture"
        const val EXTRA_VALUE = "value"
    }

    private val bundle by lazy { intent.getBundleExtra(EXTRA_BUNDLE) }
    private val sectionData by lazy {
        bundle?.getParcelableArrayList<SectionableListPreference.Section>(EXTRA_SECTIONS)
    }
    private val callback by lazy {
        val binder = bundle?.getBinder(EXTRA_CALLBACK)

        if (binder != null) {
            IActionSelectedCallback.Stub.asInterface(binder)
        } else {
            null
        }
    }
    private val gesture by lazy {
        bundle?.getString(EXTRA_GESTURE)
    }
    private val value by lazy {
        bundle?.getString(EXTRA_VALUE)
    }

    override val adapter by lazy {
        ActionSelectAdapter(value) {
            when (it.res.toString()) {
                actionHolder.typeRootKeycode.toString(),
                actionHolder.typeRootDoubleKeycode.toString(),
                actionHolder.typeRootLongKeycode.toString() -> {
                    val input = View.inflate(this, R.layout.text_input, null)

                    MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.root_send_keycode_no_format)
                            .setView(input)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                val keyCode = input.input.text
                                if (keyCode != null && keyCode.isNotBlank()) {
                                    val text = keyCode.toString().toInt()
                                    prefManager.putKeycode(gesture!!, text)
                                    handleFinish(it)
                                }
                            }
                            .show()
                }
                else -> handleFinish(it)
            }
        }
    }

    private fun handleFinish(it: ActionInfo) {
        callback?.onActionInfoSelected(it)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        list.removeItemDecorationAt(0)
        list.addItemDecoration(CategoryDividerDecoration(this))
    }

    override fun canRun(): Boolean {
        return sectionData != null && sectionData?.isNotEmpty() == true
    }

    override fun loadAppList(): ArrayList<ActionInfo> {
        val ret = ArrayList<ActionInfo>()

        sectionData?.forEach {
            ret.add(ActionInfo(it.title, null, true))

            it.entryNames.forEachIndexed { index, s ->
                ret.add(ActionInfo(s, it.entryValues[index], false))
            }
        }

        return ret
    }

    override fun loadAppInfo(info: ActionInfo): ActionInfo {
        return info
    }

    override fun filter(query: String): List<ActionInfo> {
        return ArrayList(origAppSet).filter {
            it.isHeader || it.label.toString().toLowerCase(Locale.getDefault()).contains(query.toLowerCase(Locale.getDefault()))
        }
    }

    class CategoryDividerDecoration(context: Context) : DividerItemDecoration(context, RecyclerView.VERTICAL) {
        private val bounds = Rect()

        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            c.save()

            val childCount = parent.childCount

            val left: Int
            val right: Int
            //noinspection AndroidLintNewApi - NewApi lint fails to handle overrides.
            if (parent.clipToPadding) {
                left = parent.paddingLeft
                right = parent.width - parent.paddingRight
                c.clipRect(left, parent.paddingTop, right,
                        parent.height - parent.paddingBottom)
            } else {
                left = 0
                right = parent.width
            }

            for (i in 1 until childCount) {
                val child = parent.getChildAt(i)
                val item = parent.findContainingViewHolder(child)
                if (item is ActionSelectAdapter.HeaderViewHolder) {
                    parent.getDecoratedBoundsWithMargins(child, bounds)
                    val bottom = bounds.top + Math.round(child.translationY) + drawable!!.intrinsicHeight
                    val top = bounds.top
                    drawable!!.setBounds(left, top, right, bottom)
                    drawable!!.draw(c)
                }
            }

            c.restore()
        }
    }
}