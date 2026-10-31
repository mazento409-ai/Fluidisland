package com.s01y.fluidisland

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

class IslandService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("island", "Fluid Island", NotificationManager.IMPORTANCE_MIN)
        )
        val n = Notification.Builder(this, "island")
            .setContentTitle("Fluid Island يعمل")
            .setContentText("الكبسولة جاهزة لعرض التنبيهات")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
        IslandController.attach(this)
        return START_STICKY
    }

    override fun onDestroy() {
        IslandController.detach()
        super.onDestroy()
    }
}
