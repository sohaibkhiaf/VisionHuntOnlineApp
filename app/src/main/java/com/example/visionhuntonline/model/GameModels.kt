package com.example.visionhuntonline.model

import android.graphics.RectF

enum class Rarity(val displayNameAr: String, val points: Int) {
    COMMON("شائع", 10),
    UNCOMMON("غير شائع", 20),
    RARE("نادر", 30)
}

data class TaskDefinition(
    val id: String,
    val label: String,
    val displayNameAr: String,
    val displayNameEn: String,
    val rarity: Rarity,
    val points: Int = rarity.points,
    val confidenceThreshold: Float = 0.85f
)

data class Detection(
    val classId: Int,
    val label: String,
    val score: Float,
    val boundingBox: RectF
)

data class GameState(
    val version: Int = 1,
    val completedTaskIds: Set<String> = emptySet()
)

data class GameProgress(
    val score: Int,
    val completedCount: Int,
    val totalTasks: Int,
    val isCompleted: Boolean
)
