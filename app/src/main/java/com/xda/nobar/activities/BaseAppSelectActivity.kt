package com.xda.nobar.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import com.github.lzyzsd.circleprogress.ArcProgress
import com.xda.nobar.App
import com.xda.nobar.R
import com.xda.nobar.adapters.BaseSelectAdapter
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.*

/**
 * Base activity for all app selection activities
 * Manages the basic logic of each
 */
abstract class BaseAppSelectActivity<ListItem : Any, Info: Parcelable> : AppCompatActivity(), SearchView.OnQueryTextListener {
    internal companion object {
        const val APPINFO = "app_info"
        const val EXTRA_KEY = "key"
    }

    internal abstract val adapter: BaseSelectAdapter<Info>

    internal var isCreated = false

    internal val app by lazy { application as App }

    internal val origAppSet = ArrayList<Info>()
    internal val list by lazy { findViewById<RecyclerView>(R.id.list) }
    internal val loader by lazy { findViewById<ArcProgress>(R.id.progress) }

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
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_done_black_24dp)
        }

        setContentView(R.layout.activity_app_launch_select)

        list.layoutManager = LinearLayoutManager(this, LinearLayout.VERTICAL, false)
        list.addItemDecoration(DividerItemDecoration(list.context, (list.layoutManager as LinearLayoutManager).orientation))

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
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
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

    internal fun reloadList() {
        loader.visibility = View.VISIBLE
        list.visibility = View.GONE

        adapter.clear()

        val thread = Schedulers.io()
        Observable.fromCallable { loadAppList() }
                .subscribeOn(thread)
                .observeOn(thread)
                .subscribe {
                    it.forEach { info ->
                        val appInfo = loadAppInfo(info)

                        if (appInfo != null) {
                            if (shouldAddInfo(appInfo)) {
                                adapter.add(appInfo)
                                origAppSet.add(appInfo)
                            }
                        }

                        val index = it.indexOf(info)
                        val percent = (index.toFloat() / it.size.toFloat() * 100).toInt()

                        runOnUiThread {
                            loader.progress = percent
                        }
                    }

                    runOnUiThread {
                        isCreated = true

                        list.adapter = adapter
                        loader.visibility = View.GONE
                        list.visibility = View.VISIBLE

                        try {
                            searchItem.isVisible = true
                        } catch (e: UninitializedPropertyAccessException) {}
                    }
                }
    }

    internal fun getKey() = intent.getStringExtra(EXTRA_KEY)

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
    internal abstract fun filter(query: String): ArrayList<Info>
}