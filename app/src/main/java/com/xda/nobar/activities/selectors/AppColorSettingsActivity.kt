package com.xda.nobar.activities.selectors

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import com.xda.nobar.R
import com.xda.nobar.data.ColoredAppData
import com.xda.nobar.util.prefManager
import jp.wasabeef.recyclerview.animators.LandingAnimator
import kotlinx.android.synthetic.main.activity_app_color_settings.*
import kotlinx.android.synthetic.main.colored_app_item.view.*
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet


class AppColorSettingsActivity : AppCompatActivity(), ColorPickerDialogListener, CoroutineScope by MainScope() {
    companion object {
        const val REQ_COLOR = 1001
    }

    val adapter = Adapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_app_color_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        setTitle(R.string.per_app_colors)

        val layoutManager = LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false)
        app_list.layoutManager = layoutManager
        app_list.adapter = adapter
        app_list.itemAnimator = LandingAnimator()

        val touchHelper = ItemTouchHelper(SwipeToDeleteCallback())
        touchHelper.attachToRecyclerView(app_list)

        add.setOnClickListener {
            startActivityForResult(
                    Intent(this, AppLaunchSelectActivity::class.java).apply {
                        putExtra(AppLaunchSelectActivity.EXTRA_INCLUDE_ALL_APPS, true)
                        putExtra(AppLaunchSelectActivity.EXTRA_TITLE, resources.getString(R.string.select_app))
                    },
                    REQ_COLOR)
        }

        launch {
            val items = loadItems()

            progress.visibility = View.GONE
            app_list.visibility = View.VISIBLE
            add.visibility = View.VISIBLE
            adapter.addItems(items)
        }
    }

    private suspend fun loadItems() = withContext(Dispatchers.IO) {
        ArrayList<ColoredAppData>()
                .apply { prefManager.loadColoredApps(this) }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_COLOR) {
            if (resultCode == Activity.RESULT_OK) {
                val packageName = data!!.getStringExtra(AppLaunchSelectActivity.EXTRA_PACKAGE)
                val colorData = ColoredAppData(
                        packageName,
                        Color.WHITE
                )

                adapter.addItem(colorData)
                adapter.persistItems()
            }
        }
    }

    override fun onColorSelected(dialogId: Int, color: Int) {
        adapter.updateItem(dialogId, color)
        adapter.persistItems()
    }

    override fun onDialogDismissed(dialogId: Int) {

    }

    override fun onColorReset(dialogId: Int) {
        adapter.updateItem(dialogId, Color.WHITE)
        adapter.persistItems()
    }

    override fun onPause() {
        super.onPause()

        adapter.persistItems()
    }

    override fun onDestroy() {
        super.onDestroy()

        cancel()
    }

    inner class SwipeToDeleteCallback :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        private val icon = ContextCompat.getDrawable(this@AppColorSettingsActivity, R.drawable.ic_delete_white_24dp)!!
        private val background = ColorDrawable(Color.RED)

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val item = adapter.removeItemAt(viewHolder.adapterPosition)

            val snackBar = Snackbar.make(app_list, R.string.deleted, BaseTransientBottomBar.LENGTH_LONG)
            snackBar.setAction(R.string.undo) {
                adapter.addItem(item)
            }
            snackBar.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    if (event == DISMISS_EVENT_TIMEOUT) adapter.persistItems()
                }
            })
            snackBar.show()
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return false
        }

        override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

            val itemView = viewHolder.itemView
            val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
            val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
            val iconBottom = iconTop + icon.intrinsicHeight

            when {
                dX > 0 -> { //Swiping to the right
                    val iconLeft = itemView.left + iconMargin
                    val iconRight = itemView.left + iconMargin + icon.intrinsicWidth

                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    background.setBounds(itemView.left, itemView.top,
                            itemView.left + dX.toInt(),
                            itemView.bottom)
                }
                dX < 0 -> {//Swiping to the left
                    val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                    val iconRight = itemView.right - iconMargin

                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    background.setBounds(itemView.right + dX.toInt(),
                            itemView.top, itemView.right, itemView.bottom)
                }
                else -> { //No swipe
                    background.setBounds(0, 0, 0, 0)
                }
            }

            background.draw(c)
            icon.draw(c)
        }
    }

    inner class Adapter : RecyclerView.Adapter<Adapter.VH>() {
        private val ids = HashSet<String>()
        private val items = SortedList<ListItem>(
                ListItem::class.java,
                object : SortedList.Callback<ListItem>() {
                    override fun areItemsTheSame(item1: ListItem, item2: ListItem): Boolean {
                        return item1.packageName == item2.packageName
                    }

                    override fun onMoved(fromPosition: Int, toPosition: Int) {
                        notifyItemMoved(fromPosition, toPosition)
                    }

                    override fun onChanged(position: Int, count: Int) {
                        notifyItemRangeChanged(position, count)
                    }

                    override fun onInserted(position: Int, count: Int) {
                        notifyItemRangeInserted(position, count)
                    }

                    override fun onRemoved(position: Int, count: Int) {
                        notifyItemRangeRemoved(position, count)
                    }

                    override fun compare(o1: ListItem, o2: ListItem): Int {
                        return o1.compareTo(o2)
                    }

                    override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
                        return oldItem.packageName == newItem.packageName
                    }
                }
        )

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context)
                    .inflate(R.layout.colored_app_item, parent, false))
        }

        override fun getItemCount(): Int {
            return items.size()
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.onBind(items.get(position))
        }

        fun addItems(items: Collection<ColoredAppData>) {
            this.items.addAll(
                    items.mapNotNull {
                        try {
                            val info = packageManager.getApplicationInfo(it.packageName, 0)
                            ListItem(
                                    info,
                                    packageManager.getApplicationLabel(info),
                                    it.packageName,
                                    it.color
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.filter {
                        ids.add(it.packageName)
                    }
            )
        }

        fun addItem(item: ColoredAppData) {
            val info = packageManager.getApplicationInfo(item.packageName, 0)

            if (ids.add(item.packageName)) {
                items.add(
                        ListItem(
                                info,
                                packageManager.getApplicationLabel(info),
                                item.packageName,
                                item.color
                        )
                )
            }
        }

        fun addItem(item: ListItem) {
            if (ids.add(item.packageName)) {
                items.add(item)
            }
        }

        fun updateItem(index: Int, color: Int) {
            val item = items.get(index)
            item.color = color

            notifyItemChanged(index)
        }

        fun removeItem(item: ListItem) {
            items.remove(item)
            ids.remove(item.packageName)
        }

        fun removeItemAt(index: Int): ListItem {
            return items.removeItemAt(index).also {
                ids.remove(it.packageName)
            }
        }

        fun persistItems() {
            val list = ArrayList<ColoredAppData>()

            for (i in 0 until items.size()) {
                val item = items.get(i)
                list.add(ColoredAppData(item.packageName, item.color))
            }

            prefManager.saveColoredApps(list)
        }

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            fun onBind(data: ListItem) {
                itemView.app_icon.setImageDrawable(data.appInfo.loadIcon(packageManager))
                itemView.app_label.text = data.label
                itemView.app_package.text = data.packageName

                updateColor(data.color)

                itemView.content.setOnClickListener {
                    val colorDialog = ColorPickerDialog.Builder::class.java.declaredConstructors[0]
                            .apply { isAccessible = true }.newInstance() as ColorPickerDialog.Builder

                    colorDialog.setDialogTitle(R.string.select_color)
                    colorDialog.setAllowCustom(true)
                    colorDialog.setShowAlphaSlider(true)
                    colorDialog.setColor(data.color)
                    colorDialog.setDialogId(adapterPosition)

                    colorDialog.show(this@AppColorSettingsActivity)
                }
            }

            private fun updateColor(color: Int) {
                (itemView.color.drawable as GradientDrawable).apply {
                    setColor(color)
                }
            }
        }

        inner class ListItem(
                val appInfo: ApplicationInfo,
                val label: CharSequence,
                val packageName: String,
                var color: Int
        ) : Comparable<ListItem> {
            override fun compareTo(other: ListItem): Int {
                return label.toString().toLowerCase(Locale.getDefault())
                        .compareTo(other.label.toString().toLowerCase(Locale.getDefault()))
            }

        }
    }
}