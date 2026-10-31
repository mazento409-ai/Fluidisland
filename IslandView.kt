package com.s01y.fluidisland

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.text.TextUtils
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import kotlin.math.min

/** Draws the capsule. The window is touch-transparent, so it never blocks the screen. */
class IslandView(context: Context) : View(context) {

    private val d = resources.displayMetrics.density

    var position = Prefs.POS_CAMERA
    var scale = 1f
    var shape = Prefs.SHAPE_NORMAL

    private var event: IslandEvent? = null
    private var progress = 0f
    private var anim: ValueAnimator? = null
    private val hideRunnable = Runnable { collapse() }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * d
    }
    private val titleP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
    }
    private val bodyP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFC4C4D0.toInt() }
    private val emojiP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val iconP = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val circleP = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val iconRect = RectF()
    private val clip = Path()

    private fun margin(): Float = when (position) {
        Prefs.POS_CAMERA -> 6f * d
        Prefs.POS_TOP -> 44f * d
        Prefs.POS_BOTTOM -> 64f * d
        else -> 0f
    }

    private fun expW(): Float {
        val w = when (shape) {
            Prefs.SHAPE_TALL -> 190f
            Prefs.SHAPE_WIDE -> 350f
            else -> 320f
        } * d * scale
        return min(w, width - 16f * d)
    }

    private fun expH(): Float = when (shape) {
        Prefs.SHAPE_TALL -> 128f
        Prefs.SHAPE_WIDE -> 52f
        else -> 72f
    } * d * scale

    fun configure(position: Int, scale: Float, shape: Int) {
        this.position = position
        this.scale = scale
        this.shape = shape
        invalidate()
    }

    fun show(e: IslandEvent) {
        event = e
        removeCallbacks(hideRunnable)
        anim?.cancel()
        anim = ValueAnimator.ofFloat(progress, 1f).apply {
            duration = 480
            interpolator = OvershootInterpolator(1.1f)
            addUpdateListener { progress = it.animatedValue as Float; invalidate() }
            start()
        }
        postDelayed(hideRunnable, e.durationMs)
    }

    private fun collapse() {
        anim?.cancel()
        anim = ValueAnimator.ofFloat(progress, 0f).apply {
            duration = 320
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { progress = it.animatedValue as Float; invalidate() }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (progress <= 0.01f) {
                        event = null
                        invalidate()
                    }
                }
            })
            start()
        }
    }

    private fun mix(color: Int, amount: Float): Int {
        val r = (Color.red(color) * amount).toInt()
        val g = (Color.green(color) * amount).toInt()
        val b = (Color.blue(color) * amount).toInt()
        return Color.rgb(r, g, b)
    }

    override fun onDraw(c: Canvas) {
        val e = event ?: return
        val p = progress
        if (p <= 0.001f) return

        val compactW = 96f * d
        val compactH = 30f * d
        val ew = expW()
        val eh = expH()
        val cw = compactW + (ew - compactW) * p
        val ch = compactH + (eh - compactH) * p
        val left = (width - cw) / 2f
        val top = when (position) {
            Prefs.POS_BOTTOM -> height - margin() - ch
            Prefs.POS_MIDDLE -> (height - ch) / 2f
            else -> margin()
        }
        rect.set(left, top, left + cw, top + ch)
        val r = min(ch / 2f, 34f * d * scale)

        // body: black fading toward a faint accent tint at the bottom
        fill.shader = LinearGradient(
            0f, top, 0f, top + ch,
            intArrayOf(Color.BLACK, mix(e.accent, 0.20f)), null, Shader.TileMode.CLAMP
        )
        c.drawRoundRect(rect, r, r, fill)

        // luminous edge
        val accentSoft = (e.accent and 0x00FFFFFF) or 0x66000000
        stroke.shader = LinearGradient(
            0f, top, 0f, top + ch,
            intArrayOf(0x66FFFFFF, 0x10FFFFFF, accentSoft), null, Shader.TileMode.CLAMP
        )
        c.drawRoundRect(rect, r, r, stroke)

        val a = ((p - 0.55f) / 0.45f).coerceIn(0f, 1f)
        if (a <= 0f) return

        c.save()
        clip.reset()
        clip.addRoundRect(rect, r, r, Path.Direction.CW)
        c.clipPath(clip)

        val s = scale
        val cy = top + ch / 2f
        val hasBody = e.text.isNotEmpty()

        titleP.textSize = (if (shape == Prefs.SHAPE_WIDE) 13f else 15f) * d * s
        bodyP.textSize = (if (shape == Prefs.SHAPE_WIDE) 11f else 12.5f) * d * s
        titleP.alpha = (255 * a).toInt()
        bodyP.alpha = (190 * a).toInt()
        val tm = titleP.fontMetrics
        val bm = bodyP.fontMetrics
        val th = tm.descent - tm.ascent
        val bh = bm.descent - bm.ascent
        val gap = 2f * d

        if (shape == Prefs.SHAPE_TALL) {
            val iconSize = 44f * d * s
            val total = iconSize + 8f * d + th + (if (hasBody) bh + gap else 0f)
            var y = cy - total / 2f
            val cx = left + cw / 2f
            iconRect.set(cx - iconSize / 2f, y, cx + iconSize / 2f, y + iconSize)
            drawIcon(c, e, iconSize, a)
            y += iconSize + 8f * d
            val avail = cw - 24f * d
            titleP.textAlign = Paint.Align.CENTER
            bodyP.textAlign = Paint.Align.CENTER
            c.drawText(fit(e.title, titleP, avail), cx, y - tm.ascent, titleP)
            if (hasBody) {
                y += th + gap
                c.drawText(fit(e.text, bodyP, avail), cx, y - bm.ascent, bodyP)
            }
        } else {
            val iconSize = eh * 0.62f
            val padL = (eh - iconSize) / 2f
            val iconLeft = left + padL
            iconRect.set(iconLeft, cy - iconSize / 2f, iconLeft + iconSize, cy + iconSize / 2f)
            drawIcon(c, e, iconSize, a)
            val textLeft = iconRect.right + 10f * d
            val avail = left + cw - padL - textLeft
            titleP.textAlign = Paint.Align.LEFT
            bodyP.textAlign = Paint.Align.LEFT
            val total = th + (if (hasBody) bh + gap else 0f)
            var y = cy - total / 2f
            c.drawText(fit(e.title, titleP, avail), textLeft, y - tm.ascent, titleP)
            if (hasBody) {
                y += th + gap
                c.drawText(fit(e.text, bodyP, avail), textLeft, y - bm.ascent, bodyP)
            }
        }
        c.restore()
    }

    private fun drawIcon(c: Canvas, e: IslandEvent, size: Float, a: Float) {
        val bmp = e.icon
        if (bmp != null) {
            iconP.alpha = (255 * a).toInt()
            c.save()
            val cr = 11f * d * scale
            val path = Path().apply { addRoundRect(iconRect, cr, cr, Path.Direction.CW) }
            c.clipPath(path)
            c.drawBitmap(bmp, null, iconRect, iconP)
            c.restore()
        } else {
            circleP.color = (e.accent and 0x00FFFFFF) or ((70 * a).toInt() shl 24)
            c.drawCircle(iconRect.centerX(), iconRect.centerY(), size / 2f, circleP)
            emojiP.textSize = size * 0.5f
            emojiP.alpha = (255 * a).toInt()
            val fm = emojiP.fontMetrics
            c.drawText(e.emoji, iconRect.centerX(), iconRect.centerY() - (fm.ascent + fm.descent) / 2f, emojiP)
        }
    }

    private fun fit(s: String, paint: TextPaint, avail: Float): String =
        TextUtils.ellipsize(s, paint, avail.coerceAtLeast(10f), TextUtils.TruncateAt.END).toString()
}
