package com.example.visionhuntonline.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.visionhuntonline.data.CapturedItemDbHelper
import com.example.visionhuntonline.data.CocoTasks
import com.example.visionhuntonline.data.GameRepository
import com.example.visionhuntonline.data.UserSessionManager
import com.example.visionhuntonline.model.CapturedItem
import com.example.visionhuntonline.model.Detection
import com.example.visionhuntonline.model.GameProgress
import com.example.visionhuntonline.model.GameState
import com.example.visionhuntonline.network.ApiClient
import com.example.visionhuntonline.util.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Collections

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameRepository(application)

    val gameState: StateFlow<GameState> = repository.gameState

    private val _gameProgress = MutableStateFlow(repository.getProgress())
    val gameProgress: StateFlow<GameProgress> = _gameProgress.asStateFlow()

    private val _rawDetections = MutableStateFlow<List<Detection>>(emptyList())
    val rawDetections: StateFlow<List<Detection>> = _rawDetections.asStateFlow()

    private val _gameDetections = MutableStateFlow<List<Detection>>(emptyList())
    val gameDetections: StateFlow<List<Detection>> = _gameDetections.asStateFlow()

    private val _inferenceError = MutableStateFlow<String?>(null)
    val inferenceError: StateFlow<String?> = _inferenceError.asStateFlow()

    private val _inferenceTime = MutableStateFlow(0L)
    val inferenceTime: StateFlow<Long> = _inferenceTime.asStateFlow()

    private val _isModelLoaded = MutableStateFlow(true)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    private val _showOfflineDialogEvent = MutableStateFlow(false)
    val showOfflineDialogEvent: StateFlow<Boolean> = _showOfflineDialogEvent.asStateFlow()

    private val syncingTaskIds = Collections.synchronizedSet(mutableSetOf<String>())

    fun resetOfflineDialogEvent() {
        _showOfflineDialogEvent.value = false
    }

    fun onDetectionsReceived(
        results: List<Detection>,
        inferenceTimeMs: Long,
        frameBitmap: Bitmap? = null,
        context: Context? = null
    ) {
        _rawDetections.value = results
        _inferenceTime.value = inferenceTimeMs
        _inferenceError.value = null

        val currentCompleted = gameState.value.completedTaskIds

        val matchingTasks = results.mapNotNull { detection ->
            val task = CocoTasks.TASK_BY_LABEL[detection.label.trim().lowercase()]
            if (task != null && detection.score >= task.confidenceThreshold) {
                task to detection
            } else null
        }

        _gameDetections.value = matchingTasks.map { it.second }

        val uncompletedNewTasks = matchingTasks.filter { (task, _) ->
            task.id !in currentCompleted && task.id !in syncingTaskIds
        }

        if (uncompletedNewTasks.isEmpty()) return

        val appContext = context?.applicationContext ?: getApplication<Application>().applicationContext

        // Block registering task completion if internet connection is missing
        if (!NetworkUtils.isNetworkConnected(appContext)) {
            _showOfflineDialogEvent.value = true
            return
        }

        val sessionManager = UserSessionManager(appContext)
        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            _inferenceError.value = "غير مصرح: يرجى تسجيل الدخول أولاً"
            return
        }

        val (taskToSync, detectionToSync) = uncompletedNewTasks.first()
        syncingTaskIds.add(taskToSync.id)

        viewModelScope.launch {
            try {
                val dbHelper = CapturedItemDbHelper(appContext)
                val existingItems = dbHelper.getAllItems()

                val tempImagePath = if (frameBitmap != null) {
                    try {
                        val imgFile = File(appContext.filesDir, "capture_${taskToSync.id}_${System.currentTimeMillis()}.jpg")
                        FileOutputStream(imgFile).use { out ->
                            frameBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        }
                        imgFile.absolutePath
                    } catch (e: Exception) {
                        Log.e("GameViewModel", "Error saving temp image", e)
                        ""
                    }
                } else ""

                val newItem = CapturedItem(
                    taskId = taskToSync.id,
                    taskNameAr = taskToSync.displayNameAr,
                    taskNameEn = taskToSync.displayNameEn,
                    imagePath = tempImagePath,
                    timestamp = System.currentTimeMillis(),
                    score = detectionToSync.score
                )

                val prospectiveItems = existingItems + newItem

                // Send sync request to server BEFORE completing task locally
                val syncResult = ApiClient.sync(
                    token = token,
                    completedTasks = prospectiveItems
                )

                if (syncResult.isSuccess && syncResult.data != null) {
                    // SERVER SUCCESS! Complete task locally and save DB item
                    repository.completeTask(taskToSync.id)
                    dbHelper.insertItem(newItem)

                    val newServerScore = syncResult.data.score
                    repository.updateServerScore(newServerScore)
                    _gameProgress.value = repository.getProgress()

                    Log.d("GameViewModel", "Successfully synced task ${taskToSync.id} with server. Server score: $newServerScore")
                } else {
                    // SERVER FAILED! Delete temp image, DO NOT mark completed locally
                    if (tempImagePath.isNotEmpty()) {
                        try {
                            File(tempImagePath).delete()
                        } catch (e: Exception) {
                            Log.e("GameViewModel", "Error deleting temp file on failure", e)
                        }
                    }
                    val errorMsg = syncResult.message ?: "فشلت المزامنة مع السيرفر"
                    Log.e("GameViewModel", "Server sync failed for task ${taskToSync.id}: $errorMsg")
                    _inferenceError.value = "فشل تسجيل الكشف بالسيرفر: $errorMsg"
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "Exception during detection sync with server", e)
                _inferenceError.value = e.localizedMessage ?: "خطأ في الاتصال بالسيرفر"
            } finally {
                syncingTaskIds.remove(taskToSync.id)
            }
        }
    }

    fun onDetectorError(error: String) {
        _inferenceError.value = error
        _isModelLoaded.value = false
    }

    fun resetGame(context: Context? = null) {
        val ctx = context ?: getApplication<Application>().applicationContext

        if (!NetworkUtils.isNetworkConnected(ctx)) {
            _showOfflineDialogEvent.value = true
            return
        }

        val sessionManager = UserSessionManager(ctx)
        val token = sessionManager.getToken()

        if (token.isNullOrEmpty()) {
            _inferenceError.value = "غير مصرح: يرجى تسجيل الدخول أولاً"
            return
        }

        viewModelScope.launch {
            try {
                val result = ApiClient.restart(token)
                if (result.isSuccess && result.data != null) {
                    repository.resetGame()
                    repository.updateServerScore(result.data)
                    _rawDetections.value = emptyList()
                    _gameDetections.value = emptyList()

                    try {
                        val dbHelper = CapturedItemDbHelper(ctx)
                        val items = dbHelper.getAllItems()
                        for (item in items) {
                            try {
                                val file = File(item.imagePath)
                                if (file.exists()) file.delete()
                            } catch (e: Exception) {
                                Log.e("GameViewModel", "Error deleting image file", e)
                            }
                        }
                        dbHelper.deleteAllItems()
                    } catch (e: Exception) {
                        Log.e("GameViewModel", "Error clearing captured database", e)
                    }

                    _gameProgress.value = repository.getProgress()
                    Log.d("GameViewModel", "Game restarted on server. Server score reset to: ${result.data}")
                } else {
                    Log.e("GameViewModel", "Server restart failed: ${result.message}")
                    _inferenceError.value = result.message ?: "فشلت إعادة البداية من السيرفر"
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error calling restart endpoint", e)
                _inferenceError.value = e.localizedMessage ?: "فشل الاتصال بالسيرفر لإعادة البداية"
            }
        }
    }

    fun completeTask(taskId: String) {
        // Unused directly; tasks are completed via server sync in onDetectionsReceived
    }
}
