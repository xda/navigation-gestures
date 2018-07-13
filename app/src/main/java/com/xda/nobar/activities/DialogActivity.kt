package com.xda.nobar.activities

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity

/**
 * Activity used solely for showing a dialog outside of an activity context
 */
class DialogActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_YES_RES = "yes_res"
        const val EXTRA_NO_RES = "no_res"

        var yesAct: DialogInterface.OnClickListener? = null
        var noAct: DialogInterface.OnClickListener? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val title = intent.getIntExtra(EXTRA_TITLE, android.R.string.untitled)
            val message = intent.getIntExtra(EXTRA_MESSAGE, android.R.string.untitled)
            val yesRes = intent.getIntExtra(EXTRA_YES_RES, 0)
            val noRes = intent.getIntExtra(EXTRA_NO_RES, 0)

            val builder = AlertDialog.Builder(this)
            builder.setTitle(title)
            builder.setMessage(message)

            if (yesRes != 0) builder.setPositiveButton(yesRes, yesAct)
            if (noRes != 0) builder.setNegativeButton(noRes, noAct)

            builder.setOnCancelListener {
                finish()
            }

            builder.setOnDismissListener {
                finish()
            }

            builder.show()

        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        yesAct = null
        noAct = null
    }

    /**
     * Builder class
     * Create a DialogActivity instance using this
     */
    class Builder(private val context: Context) {
        var title = android.R.string.untitled
        var message = android.R.string.untitled
        var yesRes = android.R.string.yes
        var noRes = android.R.string.no

        var yesAction: DialogInterface.OnClickListener? = null
        var noAction: DialogInterface.OnClickListener? = null

        fun start() {
            val intent = Intent(context, DialogActivity::class.java)
            intent.putExtra(EXTRA_TITLE, title)
            intent.putExtra(EXTRA_MESSAGE, message)
            intent.putExtra(EXTRA_YES_RES, yesRes)
            intent.putExtra(EXTRA_NO_RES, noRes)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            yesAct = yesAction
            noAct = noAction

            context.startActivity(intent)
        }
    }
}
