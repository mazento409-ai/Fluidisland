package com.s01y.fluidisland

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager

object IslandController {
    private var wm: WindowManager? = null
    private var view: IslandView? = null
    private var params: WindowManager.LayoutParams? = null
    private var receiver: BroadcastReceiver? = null
    private var appCtx: Context? = null
    private val handler = Handler(Looper.getMainLooper())

    fun isAttached() = view != null

    fun attach(context: Context) {
        if (view != null) return
        val app = context.applicationContext
        if (!Settings.canDrawOverlays(app)) return
        appCtx = app
        wm = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val v = IslandView(app)
        val h = (260 * app.resources.displayMetrics.density).toInt()
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            h,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        if (Build.VERSION.SDK_INT >= 30) {
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= 28) {
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        view = v
        params = lp
        applyGravity(app)
        wm?.addView(v, lp)
        applyPrefs()
        registerCharging(app)
    }

    fun detach() {
        receiver?.let { r -> runCatching { appCtx?.unregisterReceiver(r) } }
        receiver = null
        view?.let { v -> runCatching { wm?.removeView(v) } }
        view = null
        params = null
    }

    private fun applyGravity(c: Context) {
        params?.gravity = when (Prefs.position(c)) {
            Prefs.POS_MIDDLE -> Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL
            Prefs.POS_BOTTOM -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            else -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }
    }

    fun applyPrefs() {
        val c = appCtx ?: return
        val v = view ?: return
        applyGravity(c)
        v.configure(Prefs.position(c), Prefs.size(c), Prefs.shape(c))
        params?.let { runCatching { wm?.updateViewLayout(v, it) } }
    }

    fun show(e: IslandEvent) {
        handler.post { view?.show(e) }
    }

    private fun registerCharging(app: Context) {
        val r = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != Intent.ACTION_POWER_CONNECTED) return
                val bi = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = bi?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = bi?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                val pct = if (level >= 0) level * 100 / scale else -1
                show(
                    IslandEvent(
                        "يتم الشحن",
                        if (pct >= 0) "$pct%" else "",
                        null, 0xFF34C759.toInt(), 3500L, "⚡"
                    )
                )
            }
        }
        val f = IntentFilter(Intent.ACTION_POWER_CONNECTED)
        if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(r, f, Context.RECEIVER_EXPORTED)
        else app.registerReceiver(r, f)
        receiver = r
    }

    fun drawableToBitmap(d: Drawable, size: Int = 96): Bitmap {
        val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        d.setBounds(0, 0, size, size)
        d.draw(c)
        return b
    }
}
