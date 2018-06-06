package com.xda.nobar.activities

import android.app.Activity
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.github.lzyzsd.circleprogress.ArcProgress
import com.xda.nobar.App
import com.xda.nobar.R
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.*
import kotlin.collections.ArrayList

class AppLaunchSelectActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_KEY = "key"
        const val EXTRA_RESULT_DISPLAY_NAME = "name"
    }

    private lateinit var app: App
    private lateinit var loader: ArcProgress
    private lateinit var list: RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent == null || !intent.hasExtra(EXTRA_KEY)) {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        app = application as App

        setContentView(R.layout.activity_app_launch_select)

        loader = findViewById(R.id.progress)
        list = findViewById(R.id.list)

        list.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
        list.addItemDecoration(DividerItemDecoration(list.context, (list.layoutManager as LinearLayoutManager).orientation))

        val selectionListener = object : AppSelectedListener {
            override fun onAppSelected(info: AppInfo) {
                PreferenceManager.getDefaultSharedPreferences(this@AppLaunchSelectActivity)
                        .edit()
                        .putString("${intent.getStringExtra(EXTRA_KEY)}_package", "${info.packageName}/${info.activity}")
                        .apply()

                val resultIntent = Intent()
                resultIntent.putExtra(EXTRA_KEY, intent.getStringExtra(EXTRA_KEY))
                resultIntent.putExtra(EXTRA_RESULT_DISPLAY_NAME, info.displayName)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }

        Observable.fromCallable { getLauncherPackagesAsync() }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    val apps = ArrayList<AppInfo>()
                    it.forEach { info ->
                        apps.add(AppInfo(info.activityInfo.packageName,
                                info.activityInfo.name,
                                info.loadLabel(packageManager).toString(),
                                info.loadIcon(packageManager)))

                        val index = it.indexOf(info)
                        val percent = (index.toFloat() / it.size.toFloat() * 100).toInt()

                        runOnUiThread {
                            loader.progress = percent
                        }
                    }

                    val adapter = Adapter(apps, selectionListener)

                    runOnUiThread {
                        list.adapter = adapter
                        loader.visibility = View.GONE
                        list.visibility = View.VISIBLE
                    }
                }
    }

    override fun onBackPressed() {
        val resultIntent = Intent()
        resultIntent.putExtra(EXTRA_KEY, intent.getStringExtra(EXTRA_KEY))
        setResult(Activity.RESULT_CANCELED, resultIntent)
        finish()
    }

    private fun getLauncherPackagesAsync(): ArrayList<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val list = packageManager.queryIntentActivities(intent, 0)
        Collections.sort(list, ResolveInfo.DisplayNameComparator(packageManager))

        return ArrayList(list)
    }

    class AppInfo(val packageName: String, val activity: String, val displayName: String, val icon: Drawable)

    class Adapter(private val apps: ArrayList<AppInfo>, private val listener: AppSelectedListener) : RecyclerView.Adapter<Adapter.VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context).inflate(R.layout.app_info, parent, false))
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val app = apps[position]
            val view = holder.view

            val title = view.findViewById<TextView>(R.id.title)
            val icon = view.findViewById<ImageView>(R.id.icon)

            title.text = app.displayName

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
                val iconDrawable = app.icon
                if (iconDrawable is AdaptiveIconDrawable) {
                    icon.background = iconDrawable.foreground
                } else {
                    icon.background = iconDrawable
                }
            } else {
                icon.background = app.icon
            }

            view.setOnClickListener {
                listener.onAppSelected(app)
            }
        }

        override fun getItemCount(): Int {
            return apps.size
        }

        class VH(val view: View) : RecyclerView.ViewHolder(view)
    }

    interface AppSelectedListener {
        fun onAppSelected(info: AppInfo)
    }
}
