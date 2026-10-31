package com.s01y.fluidisland

import android.content.Context
import java.security.MessageDigest

object Prefs {
    const val POS_CAMERA = 0
    const val POS_TOP = 1
    const val POS_MIDDLE = 2
    const val POS_BOTTOM = 3

    const val SHAPE_NORMAL = 0
    const val SHAPE_TALL = 1
    const val SHAPE_WIDE = 2

    // SHA-256 of the PRO code (the code itself is not stored in the app)
    private const val PRO_HASH = "8307d2d764eea9bbf6bbf147d5b2ef2475119120a5f6add18974215a785013f0"

    private fun sp(c: Context) =
        c.applicationContext.getSharedPreferences("island", Context.MODE_PRIVATE)

    fun enabled(c: Context) = sp(c).getBoolean("enabled", false)
    fun setEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean("enabled", v).apply()

    fun position(c: Context) = sp(c).getInt("position", POS_CAMERA)
    fun setPosition(c: Context, v: Int) = sp(c).edit().putInt("position", v).apply()

    fun isPro(c: Context) = sp(c).getBoolean("pro", false)

    /** Stored values are ignored while PRO is locked. */
    fun size(c: Context) = if (isPro(c)) sp(c).getFloat("size", 1f) else 1f
    fun setSize(c: Context, v: Float) = sp(c).edit().putFloat("size", v).apply()

    fun shape(c: Context) = if (isPro(c)) sp(c).getInt("shape", SHAPE_NORMAL) else SHAPE_NORMAL
    fun setShape(c: Context, v: Int) = sp(c).edit().putInt("shape", v).apply()

    fun rawSize(c: Context) = sp(c).getFloat("size", 1f)
    fun rawShape(c: Context) = sp(c).getInt("shape", SHAPE_NORMAL)

    fun tryUnlock(c: Context, code: String): Boolean {
        val ok = sha256(code.trim().uppercase()) == PRO_HASH
        if (ok) sp(c).edit().putBoolean("pro", true).apply()
        return ok
    }

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
