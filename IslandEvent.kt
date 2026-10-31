package com.s01y.fluidisland

import android.graphics.Bitmap

data class IslandEvent(
    val title: String,
    val text: String,
    val icon: Bitmap?,
    val accent: Int,
    val durationMs: Long = 4000L,
    val emoji: String = "🔔"
)
