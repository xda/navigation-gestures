package com.xda.nobar.activities.selectors

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rey.material.widget.ProgressView
import com.xda.nobar.R
import com.xda.nobar.adapters.BaseSelectAdapter
import com.xda.nobar.util.logicScope
import jp.wasabeef.recyclerview.animators.LandingAnimator
import kotlinx.coroutines.launch

/**
 * Base activity for all app selection activities
 * Manages the basic logic of each
 */
@Suppress("DeferredResultUnused")
abstract class BaseAppSelectActivity<ListItem : Any, Info : Parcelable> : AppCompatActivity(), SearchView.OnQueryTextListener {
    internal companion object {
        const val APPINFO = "app_info"
        const val EXTRA_KEY = "key"
    }

    internal abstract val adapter: BaseSelectAdapter<Info, out BaseSelectAdapter.VH>

    internal var isCreated = false

    internal val origAppSet = ArrayList<Info>()
    internal val list by lazy { findViewById<RecyclerView>(R.id.list) }
    internal val loader by lazy { findViewById<ProgressView>(R.id.progress) }
    internal val key: String?
        get() = intent.getStringExtra(EXTRA_KEY)

    internal lateinit var searchItem: MenuItem

    internal abstract fun loadAppList(): ArrayList<ListItem>
    internal abstract fun loadAppInfo(info: ListItem): Info?

    /**
     * Override this to define whether or not the activity should run
     * Called from #onCreate()
     */
    internal open fun canRun() = true

    internal open fun shouldAddInfo(appInfo: Info) = true

    internal open fun showUpAsCheckMark() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!canRun()) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        if (showUpAsCheckMark()) {
            supportActionBar?.setDisplayShowCustomEnabled(true)
            supportActionBar?.setHomeAsUpIndicator(R.drawable.done)
        }

        setContentView(R.layout.activity_app_launch_select)

        list.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        list.addItemDecoration(DividerItemDecoration(list.context, (list.layoutManager as LinearLayoutManager).orientation))
        list.itemAnimator = LandingAnimator()
        list.adapter = adapter

        reloadList()
    }

    /**
     * Create and add search button to action bar
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)

        searchItem = menu.findItem(R.id.action_search)
        if (!isCreated) searchItem.isVisible = false
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(this)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> onUpPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    open fun onUpPressed() {
        onBackPressed()
    }

    override fun onBackPressed() {
        finish()
    }

    /**
     * Update the app list when the search text changes
     */
    override fun onQueryTextChange(newText: String): Boolean {
        val list = filter(newText)
        adapter.replaceAll(list)
        this.list.scrollToPosition(0)
        return true
    }

    /**
     * No-op
     */
    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onDestroy() {
        super.onDestroy()

        isCreated = false
    }

    fun superOnBackPressed() {
        super.onBackPressed()
    }

    @SuppressLint("CheckResult")
    internal fun reloadList() {
        logicScope.launch {
            runOnUiThread {
                loader.visibility = View.VISIBLE
                list.visibility = View.GONE

                adapter.clear()
            }

            val newList = ArrayList<Info>()

            val appList = loadAppList()
            appList.forEach { info ->
                val appInfo = loadAppInfo(info)

                if (appInfo != null) {
                    if (shouldAddInfo(appInfo)) {
                        newList.add(appInfo)
                        origAppSet.add(appInfo)
                    }
                }

                val index = appList.indexOf(info)
                val percent = index.toFloat() / appList.size.toFloat()

                runOnUiThread {
                    loader.progress = percent
                }
            }

            runOnUiThread {
                isCreated = true

                loader.visibility = View.GONE
                list.visibility = View.VISIBLE
                adapter.add(newList)

                try {
                    searchItem.isVisible = true
                } catch (e: UninitializedPropertyAccessException) {
                }
            }
        }
    }

    internal fun passAppInfo(intent: Intent, info: Info) {
        val bundle = Bundle()
        bundle.putParcelable(APPINFO, info)
        intent.putExtra(APPINFO, bundle)
    }

    internal fun getPassedAppInfo(): Info? = intent.getBundleExtra(APPINFO).getParcelable(APPINFO) as Info?

    /**
     * Filter logic for the search function
     * Matches both display names and package names
     */
    internal abstract fun filter(query: String): List<Info>
}