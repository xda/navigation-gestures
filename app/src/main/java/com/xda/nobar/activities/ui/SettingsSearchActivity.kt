package com.xda.nobar.activities.ui

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import com.xda.nobar.R
import com.xda.nobar.data.SettingsIndex
import com.xda.nobar.util.app

class SettingsSearchActivity : AppCompatActivity(), SearchView.OnQueryTextListener, NavController.OnDestinationChangedListener {
    private lateinit var searchItem: MenuItem

    private val listUpdateListeners = ArrayList<ListUpdateListener>()
    private val imm by lazy { getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        findNavController(R.id.search_host).addOnDestinationChangedListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)

        searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(this)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Update the app list when the search text changes
     */
    override fun onQueryTextChange(newText: String): Boolean {
        val list = app.settingsIndex.search(newText)
        listUpdateListeners.forEach { it.onListUpdate(list) }
        return true
    }

    /**
     * No-op
     */
    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        if (destination.id == R.id.searchFragment) {
            if (::searchItem.isInitialized) {
                searchItem.isVisible = true
            }
            setTitle(R.string.app_name)
        } else {
            if (::searchItem.isInitialized) {
                searchItem.actionView?.clearFocus()

                searchItem.isVisible = false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        findNavController(R.id.search_host).removeOnDestinationChangedListener(this)
    }

    fun addListUpdateListener(listener: ListUpdateListener) {
        listUpdateListeners.add(listener)
    }

    fun removeListUpdateListener(listener: ListUpdateListener) {
        listUpdateListeners.remove(listener)
    }

    interface ListUpdateListener {
        fun onListUpdate(newList: List<SettingsIndex.SettingsItem>)
    }
}