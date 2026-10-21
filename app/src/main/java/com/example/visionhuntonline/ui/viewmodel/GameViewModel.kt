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

    fun resetOfflineDialogEvent() {
        _showOfflineDialogEvent.value = false
    }

    fun onDetectionsReceived(results: List<Detection>, inferenceTimeMs: Long, frameBitmap: Bitmap? = null, context: Context? = null) {
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

        val uncompletedNewTasks = matchingTasks.filter { it.first.id !in currentCompleted }

        // Block registering task completion if internet connection is missing
        if (uncompletedNewTasks.isNotEmpty()) {
            val isConnected = context?.let { NetworkUtils.isNetworkConnected(it) } ?: true
            if (!isConnected) {
                _showOfflineDialogEvent.value = true
                return
            }
        }

        var progressChanged = false
        val appContext = context?.applicationContext
        val dbHelper = appContext?.let { CapturedItemDbHelper(it) }

        for ((task, detection) in matchingTasks) {
            if (task.id !in currentCompleted) {
                if (repository.completeTask(task.id)) {
                    progressChanged = true
                    // Capture image and save to database
                    if (appContext != null && frameBitmap != null) {
                        try {
                            val imgFile = File(appContext.filesDir, "capture_${task.id}_${System.currentTimeMillis()}.jpg")
                            FileOutputStream(imgFile).use { out ->
                                frameBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                            }
                            dbHelper?.insertItem(
                                CapturedItem(
                                    taskId = task.id,
                                    taskNameAr = task.displayNameAr,
                                    taskNameEn = task.displayNameEn,
                                    imagePath = imgFile.absolutePath,
                                    timestamp = System.currentTimeMillis(),
                                    score = detection.score
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("GameViewModel", "Error saving captured image", e)
                        }
                    }
                }
            }
        }

        if (progressChanged) {
            _gameProgress.value = repository.getProgress()
            syncGameState(appContext)
        } else if (_gameProgress.value.score != repository.getProgress().score) {
            _gameProgress.value = repository.getProgress()
        }
    }

    private fun syncGameState(context: Context?) {
        val ctx = context ?: getApplication<Application>().applicationContext
        val sessionManager = UserSessionManager(ctx)
        val token = sessionManager.getToken()

        if (token.isNullOrEmpty()) {
            Log.d("GameViewModel", "Sync skipped: User not logged in or token missing")
            return
        }

        viewModelScope.launch {
            try {
                val currentScore = repository.getProgress().score

                val syncResult = ApiClient.sync(
                    token = token,
                    score = currentScore
                )

                if (syncResult.isSuccess) {
                    Log.d("GameViewModel", "Score synced successfully with server: ${syncResult.message}")
                } else {
                    Log.e("GameViewModel", "Score sync failed: ${syncResult.message}")
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error syncing score with server", e)
            }
        }
    }

    fun onDetectorError(error: String) {
        _inferenceError.value = error
        _isModelLoaded.value = false
    }

    fun resetGame(context: Context? = null) {
        repository.resetGame()
        _gameProgress.value = repository.getProgress()
        _rawDetections.value = emptyList()
        _gameDetections.value = emptyList()

        context?.let { ctx ->
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
        }

        syncGameState(context)
    }

    fun completeTask(taskId: String) {
        if (repository.completeTask(taskId)) {
            _gameProgress.value = repository.getProgress()
            syncGameState(null)
        }
    }
}
