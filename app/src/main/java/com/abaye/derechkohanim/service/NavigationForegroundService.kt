package com.abaye.derechkohanim.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.app.Service
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.abaye.derechkohanim.MainActivity
import com.abaye.derechkohanim.R

/**
 * Scaffolding for background navigation. It owns the persistent notification and the
 * foreground service type the platform requires from Android 14 onwards. Location updates
 * are not wired in yet - see README for what is still missing.
 */
class NavigationForegroundService : Service() {

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }

            else -> startForegroundWithNotification(intent?.getStringExtra(EXTRA_SUMMARY))
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification(summary: String?) {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(summary.orEmpty())
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .build()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    companion object {
        const val CHANNEL_ID = "navigation"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP = "com.abaye.derechkohanim.STOP_NAVIGATION"
        private const val EXTRA_SUMMARY = "summary"

        fun start(context: Context, summary: String) {
            val intent = Intent(context, NavigationForegroundService::class.java)
                .putExtra(EXTRA_SUMMARY, summary)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, NavigationForegroundService::class.java)
                .setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
