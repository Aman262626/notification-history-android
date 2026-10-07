package com.aman.notifhistory

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotifListener : NotificationListenerService() {

    private val last = HashMap<String, String>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            handle(sbn)
        } catch (e: Exception) {
            // never crash the listener
        }
    }

    private fun handle(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val n = sbn.notification ?: return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val ignoreOngoing = getSharedPreferences("prefs", MODE_PRIVATE).getBoolean("ignore_ongoing", true)
        if (ignoreOngoing && (n.flags and Notification.FLAG_ONGOING_EVENT != 0)) return

        val e = n.extras
        val title = e.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        var text = e.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val big = e.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        if (big.length > text.length) text = big
        val lines = e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (lines != null && lines.isNotEmpty() && text.isBlank()) {
            text = lines.joinToString(System.lineSeparator()) { it.toString() }
        }
        if (title.isBlank() && text.isBlank()) return

        val sig = title + "|" + text
        if (last[sbn.key] == sig) return
        if (last.size > 500) last.clear()
        last[sbn.key] = sig

        val pm = packageManager
        val label = try {
            pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
        } catch (ex: Exception) {
            sbn.packageName
        }

        val item = Notif(0, sbn.packageName, label, title, text, sbn.postTime)
        val db = NotifDb.get(this)
        NotifDb.io.execute {
            db.insert(item)
            NotifDb.main.post { NotifDb.onChange?.invoke() }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        try {
            requestRebind(ComponentName(this, NotifListener::class.java))
        } catch (e: Exception) {
        }
    }
}
