package com.xda.nobar.activities.selectors

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import com.xda.nobar.R
import com.xda.nobar.data.ColoredAppData
import com.xda.nobar.util.prefManager
import kotlinx.android.synthetic.main.activity_app_color_settings.*
import kotlinx.android.synthetic.main.colored_app_item.view.*
import java.util.*
import kotlin.collections.ArrayList


class AppColorSettingsActivity : AppCompatActivity(), ColorPickerDialogListener {
    companion object {
        const val REQ_COLOR = 1001
    }

    val adapter = Adapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_app_color_settings)

        adapter.addItems(ArrayList<ColoredAppData>()
                .apply { prefManager.loadColoredApps(this) })

        val layoutManager = LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false)
        app_list.layoutManager = layoutManager
        app_list.adapter = adapter

        val touchHelper = ItemTouchHelper(SwipeToDeleteCallback(adapter, this))
        touchHelper.attachToRecyclerView(app_list)

        add.setOnClickListener {
            startActivityForResult(
                    Intent(this, AppLaunchSelectActivity::class.java).apply {
                        putExtra(AppLaunchSelectActivity.EXTRA_INCLUDE_ALL_APPS, true)
                    },
                    REQ_COLOR)
        }
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

    class SwipeToDeleteCallback(private val adapter: Adapter, context: Context) :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        private val icon = ContextCompat.getDrawable(context, R.drawable.ic_delete_white_24dp)!!
        private val background = ColorDrawable(Color.RED)

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            adapter.removeItemAt(viewHolder.adapterPosition)
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
                    val iconLeft = itemView.left + iconMargin + icon.intrinsicWidth
                    val iconRight = itemView.left + iconMargin
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
        private val items = SortedList<ListItem>(
                ListItem::class.java,
                object : SortedList.Callback<ListItem>() {
                    override fun areItemsTheSame(item1: ListItem, item2: ListItem): Boolean {
                        return item1 == item2
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
                    items.map {
                        val info = packageManager.getApplicationInfo(it.packageName, 0)
                        ListItem(
                                info,
                                packageManager.getApplicationLabel(info),
                                it.packageName,
                                it.color
                        )
                    }
            )
        }

        fun addItem(item: ColoredAppData) {
            val info = packageManager.getApplicationInfo(item.packageName, 0)

            items.add(
                    ListItem(
                            info,
                            packageManager.getApplicationLabel(info),
                            item.packageName,
                            item.color
                    )
            )
        }

        fun updateItem(index: Int, color: Int) {
            val item = items.get(index)
            item.color = color

            notifyItemChanged(index)
        }

        fun removeItem(item: ListItem) {
            items.remove(item)
        }

        fun removeItemAt(index: Int) {
            items.removeItemAt(index)
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

                itemView.setOnClickListener {
                    val colorDialog = ColorPickerDialog.Builder::class.java.declaredConstructors[0]
                            .apply { isAccessible = true }.newInstance() as ColorPickerDialog.Builder

                    colorDialog.setDialogTitle(R.string.select_color)
                    colorDialog.setAllowCustom(true)
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