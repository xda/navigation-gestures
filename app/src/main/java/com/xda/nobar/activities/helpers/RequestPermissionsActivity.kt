package com.xda.nobar.activities.helpers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
 * Use this activity to request permissions from a non-activity context
 */
class RequestPermissionsActivity : AppCompatActivity() {
    companion object {
        const val ACTION_RESULT = "com.xda.nobar.action.RESULT"

        const val EXTRA_PERMISSIONS = "permissions"
        const val EXTRA_REQUEST_CODE = "req_code"
        const val EXTRA_CLASS_NAME = "className"
        const val EXTRA_RESULT_CODE = "resultCode"

        private const val REQ_CODE = 10000

        fun createAndStart(context: Context,
                           permissions: Array<String>,
                           className: ComponentName,
                           extras: Bundle? = null,
                           requestCode: Int = REQ_CODE) {
            val intent = Intent(context, RequestPermissionsActivity::class.java)
            intent.putExtra(EXTRA_PERMISSIONS, permissions)
            if (extras != null) intent.putExtras(extras)
            intent.putExtra(EXTRA_CLASS_NAME, className)
            intent.putExtra(EXTRA_REQUEST_CODE, requestCode)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(intent)
        }
    }

    private var className: ComponentName? = null
    private var req = REQ_CODE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.hasExtra(EXTRA_PERMISSIONS)) {
            className = intent.getParcelableExtra(EXTRA_CLASS_NAME)
            req = intent.getIntExtra(EXTRA_REQUEST_CODE, REQ_CODE)

            val permissions = intent.getStringArrayExtra(EXTRA_PERMISSIONS)

            if (permissions != null
                    && permissions.isNotEmpty()
                    && Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                requestPermissions(permissions, req)
            } else finish()
        } else finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == req) {
            val intent = Intent(ACTION_RESULT)
            intent.putExtra(EXTRA_RESULT_CODE, grantResults)
            intent.putExtras(this.intent)

            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }

        finish()
    }
}
