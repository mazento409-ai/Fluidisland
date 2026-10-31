package com.s01y.fluidisland

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import kotlin.math.roundToInt

class MainActivity : Activity() {

    private val BG = 0xFF08080D.toInt()
    private val CARD = 0xFF14141C.toInt()
    private val ACCENT = 0xFF7C5CFF.toInt()
    private val GREEN = 0xFF34C759.toInt()
    private val GOLD = 0xFFFFC53D.toInt()
    private val MUTED = 0xFF8C8C9C.toInt()

    private var scroll: ScrollView? = null
    private var codeText = ""

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BG
        window.navigationBarColor = BG
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    // ---------- helpers ----------
    private fun bg(color: Int, radius: Int, strokeColor: Int = 0) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (strokeColor != 0) setStroke(dp(1), strokeColor)
    }

    private fun tv(text: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun lp(w: Int = -1, h: Int = -2, top: Int = 0) =
        LinearLayout.LayoutParams(w, h).apply { topMargin = dp(top) }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = bg(CARD, 22, 0x14FFFFFF)
        setPadding(dp(18), dp(16), dp(18), dp(18))
        layoutParams = lp(top = 14)
    }

    private fun button(text: String, filled: Boolean, onClick: () -> Unit) = TextView(this).apply {
        this.text = text
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setTextColor(if (filled) Color.WHITE else ACCENT)
        background = if (filled) bg(ACCENT, 16) else bg(0x00000000, 16, ACCENT)
        setPadding(dp(16), dp(13), dp(16), dp(13))
        setOnClickListener { onClick() }
    }

    private fun proBadge() = TextView(this).apply {
        text = "PRO"
        textSize = 10f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.BLACK)
        background = bg(GOLD, 8)
        setPadding(dp(8), dp(2), dp(8), dp(2))
    }

    private fun sectionTitle(title: String, pro: Boolean = false): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            if (pro) {
                addView(proBadge())
                addView(View(context), LinearLayout.LayoutParams(dp(8), 1))
            }
            addView(tv(title, 16f, Color.WHITE, true))
        }

    private fun chips(
        options: List<String>, selected: Int, enabled: Boolean, onPick: (Int) -> Unit
    ): LinearLayout {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = lp(top = 10)
        }
        options.chunked(2).forEachIndexed { rowIdx, row ->
            val r = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = lp(top = if (rowIdx == 0) 0 else 8)
            }
            row.forEachIndexed { i, label ->
                val idx = rowIdx * 2 + i
                val sel = idx == selected
                r.addView(TextView(this).apply {
                    text = label
                    textSize = 14f
                    gravity = Gravity.CENTER
                    typeface = if (sel) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    setTextColor(if (sel) Color.WHITE else 0xFFB4B4C2.toInt())
                    background = if (sel) bg(ACCENT, 14) else bg(0xFF1D1D28.toInt(), 14, 0x14FFFFFF)
                    setPadding(dp(8), dp(12), dp(8), dp(12))
                    isEnabled = enabled
                    setOnClickListener { if (enabled) onPick(idx) }
                }, LinearLayout.LayoutParams(0, -2, 1f).apply {
                    marginStart = dp(if (i == 0) 0 else 4)
                    marginEnd = dp(if (i == 0 && row.size > 1) 4 else 0)
                })
            }
            col.addView(r)
        }
        return col
    }

    private fun overlayGranted() = Settings.canDrawOverlays(this)
    private fun listenerGranted() =
        Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(packageName) == true

    private fun permRow(label: String, granted: Boolean, onClick: () -> Unit) =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = lp(top = 12)
            addView(tv(label, 14f, 0xFFD8D8E4.toInt()), LinearLayout.LayoutParams(0, -2, 1f))
            if (granted) addView(tv("✓ مفعّل", 14f, GREEN, true))
            else addView(TextView(context).apply {
                text = "منح"
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                background = bg(ACCENT, 12)
                setPadding(dp(18), dp(8), dp(18), dp(8))
                setOnClickListener { onClick() }
            })
        }

    // ---------- UI ----------
    private fun render() {
        val keepY = scroll?.scrollY ?: 0
        val pro = Prefs.isPro(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(28), dp(18), dp(40))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        // header
        root.addView(tv("Fluid Island", 30f, Color.WHITE, true))
        root.addView(tv("كبسولة التنبيهات بأسلوب ColorOS 17", 14f, MUTED).apply {
            layoutParams = lp(top = 4)
        })

        // status
        val status = card()
        status.addView(sectionTitle("الحالة والصلاحيات"))
        status.addView(permRow("الظهور فوق التطبيقات", overlayGranted()) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        })
        status.addView(permRow("الوصول للإشعارات", listenerGranted()) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
        val on = Prefs.enabled(this)
        status.addView(button(if (on) "إيقاف الكبسولة" else "تشغيل الكبسولة", !on) {
            toggle(!on)
        }.apply { layoutParams = lp(top = 18) })
        status.addView(button("تجربة الكبسولة", false) {
            IslandController.attach(this)
            IslandController.show(
                IslandEvent("Fluid Island", "هذه تجربة للكبسولة", null, ACCENT, 3500L, "✨")
            )
        }.apply { layoutParams = lp(top = 10) })
        root.addView(status)

        // position (free)
        val posCard = card()
        posCard.addView(sectionTitle("مكان الكبسولة"))
        posCard.addView(
            chips(
                listOf("فوق الكاميرا", "أعلى الشاشة", "منتصف الشاشة", "أسفل الشاشة"),
                Prefs.position(this), true
            ) {
                Prefs.setPosition(this, it)
                IslandController.applyPrefs()
                previewDemo()
                render()
            }
        )
        root.addView(posCard)

        // PRO
        val proCard = card()
        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            alpha = if (pro) 1f else 0.4f
        }
        inner.addView(sectionTitle("حجم الكبسولة", true))
        val sizeLabel = tv("", 13f, MUTED).apply { layoutParams = lp(top = 6) }
        fun sizeText(scale: Float) = "الحجم: ${(scale * 100).roundToInt()}%"
        sizeLabel.text = sizeText(Prefs.rawSize(this))
        inner.addView(sizeLabel)
        val seek = SeekBar(this).apply {
            max = 60
            progress = ((Prefs.rawSize(context) - 0.8f) * 100f).roundToInt().coerceIn(0, 60)
            isEnabled = pro
            progressTintList = ColorStateList.valueOf(ACCENT)
            thumbTintList = ColorStateList.valueOf(ACCENT)
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    if (!fromUser) return
                    val sc = 0.8f + p / 100f
                    Prefs.setSize(context, sc)
                    sizeLabel.text = sizeText(sc)
                    IslandController.applyPrefs()
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) = previewDemo()
            })
            layoutParams = lp(top = 6)
        }
        inner.addView(seek)

        inner.addView(sectionTitle("شكل الكبسولة", true).apply { layoutParams = lp(top = 22) })
        inner.addView(
            chips(listOf("عادي", "نحيف وطويل", "نحيف وعريض"), Prefs.rawShape(this), pro) {
                Prefs.setShape(this, it)
                IslandController.applyPrefs()
                previewDemo()
                render()
            }
        )
        proCard.addView(inner)
        if (!pro) {
            proCard.addView(tv("🔒 مقفلة — أدخل الكود بالأسفل لفتحها", 13f, GOLD).apply {
                layoutParams = lp(top = 14)
            })
        }
        root.addView(proCard)

        // code
        val codeCard = card()
        codeCard.addView(sectionTitle("كود التفعيل"))
        val input = EditText(this).apply {
            setText(codeText)
            hint = "······"
            setHintTextColor(0xFF4A4A5A.toInt())
            setTextColor(Color.WHITE)
            textSize = 24f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            letterSpacing = 0.35f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            filters = arrayOf(InputFilter.LengthFilter(6), InputFilter.AllCaps())
            background = bg(0xFF0E0E15.toInt(), 16, 0x22FFFFFF)
            setPadding(dp(12), dp(14), dp(12), dp(14))
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            layoutParams = lp(top = 12)
            isEnabled = !pro
        }
        codeCard.addView(input)
        codeCard.addView(
            tv(
                if (pro) "🔓 PRO مفتوح" else "🔒 PRO مقفل",
                14f, if (pro) GREEN else GOLD, true
            ).apply {
                gravity = Gravity.CENTER
                layoutParams = lp(top = 12)
            }
        )
        if (!pro) {
            codeCard.addView(button("تفعيل الكود", true) {
                codeText = input.text.toString()
                if (codeText.length < 6) {
                    toast("الكود لازم يكون 6 أحرف أو أرقام")
                } else if (Prefs.tryUnlock(this, codeText)) {
                    toast("تم فتح إعدادات PRO ✓")
                    codeText = ""
                    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(input.windowToken, 0)
                    IslandController.applyPrefs()
                    render()
                } else {
                    toast("الكود غير صحيح")
                }
            }.apply { layoutParams = lp(top = 14) })
        }
        root.addView(codeCard)

        val sv = ScrollView(this).apply {
            setBackgroundColor(BG)
            isFillViewport = true
            addView(root)
        }
        scroll = sv
        setContentView(sv)
        sv.post { sv.scrollTo(0, keepY) }
    }

    private fun toggle(enable: Boolean) {
        if (enable) {
            if (!overlayGranted()) {
                toast("امنح إذن الظهور فوق التطبيقات أولاً")
                startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                )
                return
            }
            Prefs.setEnabled(this, true)
            startForegroundService(Intent(this, IslandService::class.java))
            if (!listenerGranted()) toast("فعّل الوصول للإشعارات عشان تظهر التنبيهات")
        } else {
            Prefs.setEnabled(this, false)
            stopService(Intent(this, IslandService::class.java))
            IslandController.detach()
        }
        render()
    }

    private fun previewDemo() {
        if (!IslandController.isAttached()) return
        IslandController.show(
            IslandEvent("Fluid Island", "معاينة الإعدادات", null, ACCENT, 2500L, "✨")
        )
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
