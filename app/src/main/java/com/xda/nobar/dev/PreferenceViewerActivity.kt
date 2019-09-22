package com.xda.nobar.dev

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.xda.nobar.R
import kotlinx.android.synthetic.main.activity_preference_viewer.*
import java.io.FileInputStream
import java.io.ObjectInputStream
import java.util.*

class PreferenceViewerActivity : AppCompatActivity(), SearchView.OnQueryTextListener {
    companion object {
        const val REQ_VIEW_PREFS = 1001
    }

    private val adapter = PreferenceViewerAdapter()

    private val openIntent: Intent
        get() = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/vnd.nobar.omni"

            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                    "application/nobarbak",
                    "application/octet-stream",
                    "nobar/appearance",
                    "application/vnd.nobar.appearance",
                    "nobar/behavior",
                    "application/vnd.nobar.behavior",
                    "nobar/gesture",
                    "application/vnd.nobar.gesture",
                    "nobar/omni",
                    "application/vnd.nobar.omni"
            ))
        }

    private lateinit var searchItem: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_preference_viewer)

        add_prefs.setOnClickListener {
            startActivityForResult(openIntent, REQ_VIEW_PREFS)
        }

        preference_list.adapter = adapter
        preference_list.layoutManager = LinearLayoutManager(this)
        preference_list.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQ_VIEW_PREFS -> {
                    adapter.setItems(
                            deserialize(data?.data ?: return) ?: return
                    )
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)

        searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(this)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        adapter.onSearch(newText)
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    private fun deserialize(src: Uri): HashMap<String, Any?>? {
        try {
            contentResolver.openFileDescriptor(src, "r")?.use { fd ->
                FileInputStream(fd.fileDescriptor).use { input ->
                    ObjectInputStream(input).use { ois ->
                        return ois.readObject() as HashMap<String, Any?>
                    }
                }
            }
        } catch (e: Exception) {}

        return null
    }
}