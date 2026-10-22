package com.example.visionhuntonline.data

import android.content.Context
import android.content.SharedPreferences
import com.example.visionhuntonline.model.GameProgress
import com.example.visionhuntonline.model.GameState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _gameState = MutableStateFlow(loadState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private fun loadState(): GameState {
        val saved = prefs.getStringSet(KEY_COMPLETED_TASKS, emptySet()) ?: emptySet()
        val validIds = CocoTasks.GAME_TASKS.map { it.id }.toSet()
        val filtered = saved.filter { it in validIds }.toSet()
        return GameState(version = 1, completedTaskIds = filtered)
    }

    fun completeTask(taskId: String): Boolean {
        val current = _gameState.value.completedTaskIds
        if (taskId in current) return false

        val taskExists = CocoTasks.GAME_TASKS.any { it.id == taskId }
        if (!taskExists) return false

        val updated = current + taskId
        prefs.edit().putStringSet(KEY_COMPLETED_TASKS, updated).apply()
        _gameState.value = GameState(version = 1, completedTaskIds = updated)
        return true
    }

    fun replaceCompletedTasksAndScore(completedTaskIds: Set<String>, serverScore: Int) {
        prefs.edit()
            .putStringSet(KEY_COMPLETED_TASKS, completedTaskIds)
            .putInt(KEY_SERVER_SCORE, serverScore)
            .apply()

        _gameState.value = GameState(version = 1, completedTaskIds = completedTaskIds)
    }

    fun updateServerScore(score: Int) {
        prefs.edit().putInt(KEY_SERVER_SCORE, score).apply()
    }

    fun getServerScore(): Int = prefs.getInt(KEY_SERVER_SCORE, 0)

    fun resetGame() {
        prefs.edit().remove(KEY_COMPLETED_TASKS).putInt(KEY_SERVER_SCORE, 0).apply()
        _gameState.value = GameState(version = 1, completedTaskIds = emptySet())
    }

    fun getProgress(state: GameState = _gameState.value): GameProgress {
        val completedSet = state.completedTaskIds
        val tasks = CocoTasks.GAME_TASKS

        val score = getServerScore()
        val completedCount = tasks.count { it.id in completedSet }
        val totalTasks = tasks.size
        val isCompleted = totalTasks > 0 && completedCount == totalTasks

        return GameProgress(
            score = score,
            completedCount = completedCount,
            totalTasks = totalTasks,
            isCompleted = isCompleted
        )
    }

    companion object {
        private const val PREFS_NAME = "vision_hunt_prefs"
        private const val KEY_COMPLETED_TASKS = "@vision_hunt/completed_tasks"
        private const val KEY_SERVER_SCORE = "@vision_hunt/server_score"
    }
}
