package com.s01y.fluidisland

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotifListener : NotificationListenerService() {

    private val last = HashMap<String, String>()

    override fun onListenerConnected() {
        if (Prefs.enabled(this)) IslandController.attach(this)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        last.remove(sbn.key)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!Prefs.enabled(this) || sbn.packageName == packageName) return
        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val isMedia = n.category == Notification.CATEGORY_TRANSPORT
        val isCall = n.category == Notification.CATEGORY_CALL
        if (n.flags and Notification.FLAG_ONGOING_EVENT != 0 && !isMedia && !isCall) return

        val ex = n.extras ?: return
        val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        if (title.isEmpty()) return
        val text = (ex.getCharSequence(Notification.EXTRA_TEXT)
            ?: ex.getCharSequence(Notification.EXTRA_SUB_TEXT))?.toString()?.trim().orEmpty()

        val sig = "$title|$text"
        if (last[sbn.key] == sig) return
        last[sbn.key] = sig

        IslandController.attach(this)

        val icon = runCatching {
            val large = if (isMedia) n.getLargeIcon()?.loadDrawable(this) else null
            val d = large ?: packageManager.getApplicationIcon(sbn.packageName)
            IslandController.drawableToBitmap(d)
        }.getOrNull()

        val (accent, dur) = when {
            isCall -> 0xFF34C759.toInt() to 8000L
            isMedia -> 0xFFFF4D6D.toInt() to 5000L
            else -> 0xFF8E8EFF.toInt() to 4000L
        }
        IslandController.show(IslandEvent(title, text, icon, accent, dur))
    }
}
