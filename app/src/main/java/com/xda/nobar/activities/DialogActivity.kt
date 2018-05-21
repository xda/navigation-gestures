package com.xda.nobar.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity

class DialogActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_YES_URL = "yes_url"
        const val EXTRA_NO_URL = "no_url"
        const val EXTRA_SHOW_YES = "show_yes"
        const val EXTRA_YES_RES = "yes_res"
        const val EXTRA_SHOW_NO = "show_no"
        const val EXTRA_NO_RES = "no_res"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val title = intent.getIntExtra(EXTRA_TITLE, android.R.string.untitled)
            val message = intent.getIntExtra(EXTRA_MESSAGE, android.R.string.untitled)
            val yesUrl = intent.getStringExtra(EXTRA_YES_URL)
            val noUrl = intent.getStringExtra(EXTRA_NO_URL)
            val showYes = intent.getBooleanExtra(EXTRA_SHOW_YES, true)
            val showNo = intent.getBooleanExtra(EXTRA_SHOW_NO, true)
            val yesRes = intent.getIntExtra(EXTRA_YES_RES, android.R.string.yes)
            val noRes = intent.getIntExtra(EXTRA_NO_RES, android.R.string.no)

            val builder = AlertDialog.Builder(this)
            builder.setTitle(title)
            builder.setMessage(message)

            if (showYes) builder.setPositiveButton(yesRes, { _, _ ->
                if (yesUrl != null) {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.data = Uri.parse(yesUrl)
                    startActivity(intent)
                }
                finish()
            })

            if (showNo) builder.setNegativeButton(noRes, { _, _ ->
                if (noUrl != null) {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.data = Uri.parse(noUrl)
                    startActivity(intent)
                }
                finish()
            })

            builder.setOnCancelListener {
                finish()
            }

            builder.show()

        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }

    class Builder(private val context: Context) {
        var title = android.R.string.untitled
        var message = android.R.string.untitled
        var yesRes = android.R.string.yes
        var noRes = android.R.string.no

        var yesUrl: String? = null
        var noUrl: String? = null

        var showYes = false
        var showNo = false

        fun start() {
            val intent = Intent(context, DialogActivity::class.java)
            intent.putExtra(EXTRA_TITLE, title)
            intent.putExtra(EXTRA_MESSAGE, message)
            intent.putExtra(EXTRA_YES_RES, yesRes)
            intent.putExtra(EXTRA_NO_RES, noRes)
            intent.putExtra(EXTRA_YES_URL, yesUrl)
            intent.putExtra(EXTRA_NO_URL, noUrl)
            intent.putExtra(EXTRA_SHOW_YES, showYes)
            intent.putExtra(EXTRA_SHOW_NO, showNo)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(intent)
        }
    }
}
