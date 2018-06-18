package com.xda.nobar.activities

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity

class RequestPermissionsActivity : AppCompatActivity() {
    companion object {
        const val ACTION_RESULT = "com.xda.nobar.action.RESULT"

        const val EXTRA_PERMISSIONS = "permissions"
        const val EXTRA_REQUEST_CODE = "req_code"
        const val EXTRA_CLASS_NAME = "className"
        const val EXTRA_RESULT_CODE = "resultCode"

        private const val REQ_CODE = 10000
    }

    private lateinit var className: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.hasExtra(EXTRA_PERMISSIONS) && intent.hasExtra(EXTRA_CLASS_NAME)) {
            className = intent.getParcelableExtra(EXTRA_CLASS_NAME)

            val permissions = intent.getStringArrayExtra(EXTRA_PERMISSIONS)

            if (permissions != null) {
                ActivityCompat.requestPermissions(this, permissions, REQ_CODE)
            } else finish()
        } else finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_CODE) {
            val intent = Intent(ACTION_RESULT)
            intent.component = className
            intent.putExtra(EXTRA_RESULT_CODE, resultCode)
            intent.putExtras(this.intent)

            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }
}
