package com.xda.nobar.activities.helpers

import android.content.*
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.xda.nobar.interfaces.OnDialogChoiceMadeListener

/**
 * Activity used solely for showing a dialog outside of an activity context
 */
class DialogActivity : AppCompatActivity() {
    companion object {
        const val BASE_YES_ACTION = "com.xda.nobar.intent.action.YES_"
        const val BASE_NO_ACTION = "com.xda.nobar.intent.action.NO_"
        const val BASE_DESTROY_ACTION = "com.xda.nobar.intent.action.DESTROY_"

        const val EXTRA_TITLE = "title"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_YES_RES = "yes_res"
        const val EXTRA_NO_RES = "no_res"
        const val EXTRA_HAS_YES = "has_yes"
        const val EXTRA_HAS_NO = "has_no"
        const val EXTRA_HASH = "hash"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val title = intent.getIntExtra(EXTRA_TITLE, android.R.string.untitled)
            val message = intent.getIntExtra(EXTRA_MESSAGE, android.R.string.untitled)
            val yesRes = intent.getIntExtra(EXTRA_YES_RES, 0)
            val noRes = intent.getIntExtra(EXTRA_NO_RES, 0)
            val hasYes = intent.getBooleanExtra(EXTRA_HAS_YES, false)
            val hasNo = intent.getBooleanExtra(EXTRA_HAS_NO, false)
            val hash = intent.getIntExtra(EXTRA_HASH, -1)

            val builder = AlertDialog.Builder(this)
            builder.setTitle(title)
            builder.setMessage(message)

            if (yesRes != 0) builder.setPositiveButton(yesRes) { _, _ -> if (hasYes) sendYes(hash) }
            if (noRes != 0) builder.setNegativeButton(noRes) { _, _ -> if (hasNo) sendNo(hash) }

            builder.setOnCancelListener {
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

        sendDestroy(intent.getIntExtra(EXTRA_HASH, -1))
    }

    private fun sendYes(hash: Int) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(BASE_YES_ACTION + hash))
        finish()
    }

    private fun sendNo(hash: Int) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(BASE_NO_ACTION + hash))
        finish()
    }

    private fun sendDestroy(hash: Int) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(BASE_DESTROY_ACTION + hash))
    }

    /**
     * Builder class
     * Create a DialogActivity instance using this
     */
    class Builder(private val context: Context) : BroadcastReceiver() {
        var title = android.R.string.untitled
        var message = android.R.string.untitled
        var yesRes = android.R.string.yes
        var noRes = android.R.string.no

        var yesAction: OnDialogChoiceMadeListener? = null
        var noAction: OnDialogChoiceMadeListener? = null

        private val yesBc = BASE_YES_ACTION + hashCode()
        private val noBc = BASE_NO_ACTION + hashCode()
        private val destroyBc = BASE_DESTROY_ACTION + hashCode()

        fun start() {
            val filter = IntentFilter()
            filter.addAction(yesBc)
            filter.addAction(noBc)
            filter.addAction(destroyBc)

            LocalBroadcastManager.getInstance(context).registerReceiver(this, filter)

            val intent = Intent(context, DialogActivity::class.java)
            intent.putExtra(EXTRA_TITLE, title)
            intent.putExtra(EXTRA_MESSAGE, message)
            intent.putExtra(EXTRA_YES_RES, yesRes)
            intent.putExtra(EXTRA_NO_RES, noRes)
            intent.putExtra(EXTRA_HAS_YES, yesAction != null)
            intent.putExtra(EXTRA_HAS_NO, noAction != null)
            intent.putExtra(EXTRA_HASH, hashCode())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(intent)
        }

        private fun onDestroy() {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(this)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                yesBc -> yesAction?.onDialogChoiceMade(DialogInterface.BUTTON_POSITIVE)
                noBc -> noAction?.onDialogChoiceMade(DialogInterface.BUTTON_NEGATIVE)
                destroyBc -> onDestroy()
            }
        }
    }
}
