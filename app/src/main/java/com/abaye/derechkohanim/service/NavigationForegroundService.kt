package com.abaye.derechkohanim.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import com.abaye.derechkohanim.MainActivity
import com.abaye.derechkohanim.R
import com.abaye.derechkohanim.core.Formatters
import com.abaye.derechkohanim.data.model.NavigationStatus
import com.abaye.derechkohanim.nav.NavigationEngine
import com.abaye.derechkohanim.ui.screens.nav.NavigationState
import javax.inject.Inject

/**
 * Keeps a real drive running while the app is in the background. The service holds nothing of its
 * own: the driving state and logic live in the shared @Singleton [NavigationEngine], and this
 * service exists purely to keep the process alive (a foreground service the platform will not kill)
 * and to mirror the engine's [NavigationEngine.state] into an ongoing CATEGORY_NAVIGATION
 * notification with the next instruction, distance and ETA. Tapping the notification returns to the
 * navigation screen via MainActivity (singleTask). The engine starts this service when a real drive
 * begins and stops it when the drive ends; the service also stops itself if the engine reaches a
 * terminal state on its own (e.g. arrival).
 */
@AndroidEntryPoint
class NavigationForegroundService : Service() {

    @Inject
    lateinit var engine: NavigationEngine

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observing = false

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            engine.stop()
            stopSelfAndForeground()
            return START_NOT_STICKY
        }
        // Post the first notification immediately (the platform requires it within a few seconds).
        startForegroundWithState(engine.state.value)
        observeState()
        return START_NOT_STICKY
    }

    /** Mirrors the engine's live state into the notification, and stops the service the moment the
     *  drive reaches a terminal state so the ongoing notification never outlives the drive. */
    private fun observeState() {
        if (observing) return
        observing = true
        scope.launch {
            engine.state.collect { state ->
                if (state.status.isTerminal) {
                    stopSelfAndForeground()
                    return@collect
                }
                notificationManager().notify(NOTIFICATION_ID, buildNotification(state))
            }
        }
    }

    private fun startForegroundWithState(state: NavigationState) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(state), type)
    }

    private fun buildNotification(state: NavigationState): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val maneuver = state.nextManeuver ?: state.currentManeuver
        val title = if (maneuver != null && maneuver.instruction.isNotBlank()) {
            val distance = Formatters.distance(this, state.distanceToManeuverMeters)
            "$distance · ${maneuver.instruction}"
        } else {
            getString(R.string.service_notification_title)
        }
        val text = buildString {
            append(Formatters.arrivalTime(state.durationRemainingSeconds))
            append("  ·  ")
            append(Formatters.distance(this@NavigationForegroundService, state.distanceRemainingMeters))
            append("  ·  ")
            append(Formatters.duration(this@NavigationForegroundService, state.durationRemainingSeconds))
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .build()
    }

    private fun stopSelfAndForeground() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun notificationManager(): NotificationManager =
        getSystemService(NotificationManager::class.java)

    companion object {
        const val CHANNEL_ID = "navigation"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP = "com.abaye.derechkohanim.STOP_NAVIGATION"

        fun start(context: Context) {
            val intent = Intent(context, NavigationForegroundService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, NavigationForegroundService::class.java)
                .setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}

/** ARRIVED, CANCELLED and IDLE all mean there is nothing left to keep in the foreground. */
private val NavigationStatus.isTerminal: Boolean
    get() = this == NavigationStatus.ARRIVED ||
        this == NavigationStatus.CANCELLED ||
        this == NavigationStatus.IDLE
