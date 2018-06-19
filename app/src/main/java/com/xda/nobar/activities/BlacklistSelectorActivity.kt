package com.xda.nobar.activities

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.github.lzyzsd.circleprogress.ArcProgress
import com.rey.material.widget.CheckedImageView
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.interfaces.OnAppSelectedListener
import com.xda.nobar.util.AppInfo
import com.xda.nobar.util.Utils
import com.xda.nobar.util.Utils.getBitmapDrawable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.*
import kotlin.collections.ArrayList

class BlacklistSelectorActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_WHICH = "which"

        const val FOR_BAR = "bar"
        const val FOR_NAV = "nav"
    }

    private val currentlyBlacklisted = ArrayList<String>()
    private var which: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (intent == null || !intent.hasExtra(EXTRA_WHICH)) {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        val blacklist = ArrayList<String>()

        which = intent.getStringExtra(EXTRA_WHICH)

        when (which) {
            FOR_BAR -> {
                title = resources.getText(R.string.bar_blacklist)
                Utils.loadBlacklistedBarPackages(this, blacklist)
            }
            FOR_NAV -> {
                title = resources.getText(R.string.nav_blacklist)
                Utils.loadBlacklistedNavPackages(this, blacklist)
            }
            else -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }

        currentlyBlacklisted.addAll(blacklist)

        val app = application as App
        app.refreshPremium()

        setContentView(R.layout.activity_app_launch_select)

        val loader = findViewById<ArcProgress>(R.id.progress)
        val list = findViewById<RecyclerView>(R.id.list)

        list.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
        list.addItemDecoration(DividerItemDecoration(list.context, (list.layoutManager as LinearLayoutManager).orientation))

        Observable.fromCallable { getAppsAsync() }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    val apps = ArrayList<AppInfo>()
                    it.forEach { info ->
                        apps.add(AppInfo(info.packageName,
                                "",
                                info.loadLabel(packageManager).toString(),
                                info.loadIcon(packageManager), blacklist.contains(info.packageName)))

                        val index = it.indexOf(info)
                        val percent = (index.toFloat() / it.size.toFloat() * 100).toInt()

                        runOnUiThread {
                            loader.progress = percent
                        }
                    }

                    val adapter = Adapter(apps) { info ->
                        if (info.isChecked) currentlyBlacklisted.add(info.packageName)
                        else currentlyBlacklisted.removeAll(Collections.singleton(info.packageName))
                    }

                    runOnUiThread {
                        list.adapter = adapter
                        loader.visibility = View.GONE
                        list.visibility = View.VISIBLE
                    }
                }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val resultIntent = Intent()
        resultIntent.putExtra(AppLaunchSelectActivity.EXTRA_KEY, intent.getStringExtra(AppLaunchSelectActivity.EXTRA_KEY))
        setResult(Activity.RESULT_CANCELED, resultIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()

        when (which) {
            FOR_NAV -> Utils.saveBlacklistedNavPackageList(this, currentlyBlacklisted)
            FOR_BAR -> Utils.saveBlacklistedBarPackages(this, currentlyBlacklisted)
        }
    }

    private fun getAppsAsync(): ArrayList<ApplicationInfo> {
        val list = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        Collections.sort(list, ApplicationInfo.DisplayNameComparator(packageManager))

        return ArrayList(list)
    }

    class Adapter(private val apps: ArrayList<AppInfo>,
                  private val checkListener: OnAppSelectedListener) : RecyclerView.Adapter<Adapter.VH>() {

        override fun getItemCount() = apps.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                VH(LayoutInflater.from(parent.context).inflate(R.layout.app_info_multi, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val app = apps[position]
            val view = holder.view

            val title = view.findViewById<TextView>(R.id.title)
            val icon = view.findViewById<ImageView>(R.id.icon)
            val check = view.findViewById<CheckedImageView>(R.id.checkmark)

            title.text = app.displayName

            icon.background = getBitmapDrawable(apps[position].icon, holder.view.context.resources)

            view.setOnClickListener {
                check.isChecked = !check.isChecked
                app.isChecked = check.isChecked

                checkListener.invoke(app)
            }

            check.isChecked = app.isChecked
        }


        class VH(val view: View) : RecyclerView.ViewHolder(view)
    }
}
