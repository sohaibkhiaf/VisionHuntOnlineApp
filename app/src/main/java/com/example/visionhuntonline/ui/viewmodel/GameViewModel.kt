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
import com.example.visionhuntonline.network.CurrentEventData
import com.example.visionhuntonline.network.LeaderboardPlayer
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

    private val _sessionExpiredEvent = MutableStateFlow(false)
    val sessionExpiredEvent: StateFlow<Boolean> = _sessionExpiredEvent.asStateFlow()

    private val _serverErrorSnackbarEvent = MutableStateFlow<String?>(null)
    val serverErrorSnackbarEvent: StateFlow<String?> = _serverErrorSnackbarEvent.asStateFlow()

    private val _currentEvent = MutableStateFlow<CurrentEventData?>(null)
    val currentEvent: StateFlow<CurrentEventData?> = _currentEvent.asStateFlow()

    private val _miniLeaderboard = MutableStateFlow<List<LeaderboardPlayer>>(emptyList())
    val miniLeaderboard: StateFlow<List<LeaderboardPlayer>> = _miniLeaderboard.asStateFlow()

    private val syncingTaskIds = Collections.synchronizedSet(mutableSetOf<String>())

    fun resetOfflineDialogEvent() {
        _showOfflineDialogEvent.value = false
    }

    fun resetSessionExpiredEvent() {
        _sessionExpiredEvent.value = false
    }

    fun triggerServerErrorSnackbar(message: String) {
        _serverErrorSnackbarEvent.value = message
    }

    fun resetServerErrorSnackbarEvent() {
        _serverErrorSnackbarEvent.value = null
    }

    fun fetchCurrentEvent(context: Context? = null) {
        val ctx = context ?: getApplication<Application>().applicationContext

        if (!NetworkUtils.isNetworkConnected(ctx)) return

        val token = UserSessionManager(ctx).getToken()

        viewModelScope.launch {
            try {
                val result = ApiClient.getCurrentEvent(token)
                if (result.isSuccess) {
                    _currentEvent.value = result.data
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error fetching current event", e)
            }
        }
    }

    fun fetchMiniLeaderboard(context: Context? = null) {
        val ctx = context ?: getApplication<Application>().applicationContext

        if (!NetworkUtils.isNetworkConnected(ctx)) return

        viewModelScope.launch {
            try {
                val currentUsername = UserSessionManager(ctx).getUsername()
                val currentScore = repository.getProgress().score
                var foundPlayer: LeaderboardPlayer? = null
                val allFetchedPlayers = mutableListOf<LeaderboardPlayer>()

                val page1Result = ApiClient.getLeaderboard(page = 1)
                if (page1Result.isSuccess && page1Result.data != null) {
                    val data = page1Result.data
                    allFetchedPlayers.addAll(data.players)
                    foundPlayer = data.players.find { it.username.equals(currentUsername, ignoreCase = true) }

                    if (foundPlayer == null && data.totalPages > 1) {
                        for (p in 2..data.totalPages) {
                            val pageResult = ApiClient.getLeaderboard(page = p)
                            if (pageResult.isSuccess && pageResult.data != null) {
                                allFetchedPlayers.addAll(pageResult.data.players)
                                foundPlayer = pageResult.data.players.find { it.username.equals(currentUsername, ignoreCase = true) }
                                if (foundPlayer != null) break
                            }
                        }
                    }
                }

                val targetUserPlayer = foundPlayer ?: LeaderboardPlayer(
                    rank = allFetchedPlayers.size + 1,
                    username = currentUsername,
                    score = currentScore
                )

                val selected = selectMiniLeaderboardPlayers(allFetchedPlayers, targetUserPlayer)
                _miniLeaderboard.value = selected
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error fetching mini leaderboard", e)
            }
        }
    }

    private fun selectMiniLeaderboardPlayers(
        allPlayers: List<LeaderboardPlayer>,
        userPlayer: LeaderboardPlayer
    ): List<LeaderboardPlayer> {
        val sortedPlayers = allPlayers.sortedBy { it.rank }
        if (sortedPlayers.isEmpty()) {
            return listOf(userPlayer)
        }

        val top2 = sortedPlayers.take(2)

        return if (userPlayer.rank <= 3) {
            sortedPlayers.take(3)
        } else {
            listOf(top2[0], top2[1], userPlayer)
        }
    }

    fun fetchAndSyncAccountState(context: Context? = null) {
        val ctx = context ?: getApplication<Application>().applicationContext

        fetchCurrentEvent(ctx)
        fetchMiniLeaderboard(ctx)

        if (!NetworkUtils.isNetworkConnected(ctx)) {
            Log.d("GameViewModel", "Network not connected, skipping account check")
            return
        }

        val sessionManager = UserSessionManager(ctx)
        val token = sessionManager.getToken()

        if (token.isNullOrEmpty()) {
            Log.d("GameViewModel", "Token null/empty, skipping account check")
            return
        }

        viewModelScope.launch {
            try {
                val result = ApiClient.fetchPlayerStatus(token)
                if (result.statusCode == 401) {
                    sessionManager.clearSession()
                    _sessionExpiredEvent.value = true
                    return@launch
                }

                if (result.isSuccess && result.data != null) {
                    val data = result.data

                    data.phone?.let { phone ->
                        if (phone.isNotEmpty()) sessionManager.updatePhone(phone)
                    }

                    val taskIds = data.completedTasks.mapNotNull { serverTask ->
                        CocoTasks.TASK_BY_LABEL[serverTask.objectNameEn.trim().lowercase()]?.id
                    }.toSet()

                    val dbHelper = CapturedItemDbHelper(ctx)

                    // Purge local database items and image files if event ended or tasks removed on server
                    val localItems = dbHelper.getAllItems()
                    for (item in localItems) {
                        if (item.taskId !in taskIds) {
                            try {
                                val file = File(item.imagePath)
                                if (file.exists()) file.delete()
                            } catch (e: Exception) {
                                Log.e("GameViewModel", "Error deleting local image file on sync", e)
                            }
                        }
                    }
                    dbHelper.deleteItemsNotIn(taskIds)

                    repository.replaceCompletedTasksAndScore(taskIds, data.score)
                    _gameProgress.value = repository.getProgress()

                    // Refresh mini leaderboard with updated score
                    fetchMiniLeaderboard(ctx)

                    Log.d("GameViewModel", "Account state synced from player_status.php. Score: ${data.score}, Tasks: ${taskIds.size}")
                } else {
                    Log.e("GameViewModel", "Failed to check account state: ${result.message}")
                    triggerServerErrorSnackbar(result.message ?: "تعذر الاتصال بالسيرفر جلب بيانات الحساب")
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error fetching account state from player_status.php", e)
                triggerServerErrorSnackbar("تعذر الاتصال بالسيرفر. يرجى المحاولة مرة أخرى لاحقاً.")
            }
        }
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

        val matchingTasks = results.mapNotNull { detection ->
            val task = CocoTasks.TASK_BY_LABEL[detection.label.trim().lowercase()]
            if (task != null && detection.score >= task.confidenceThreshold) {
                task to detection
            } else null
        }

        _gameDetections.value = matchingTasks.map { it.second }

        // Allow sending complete_task request on every detection, even if already marked completed locally,
        // while ignoring tasks currently awaiting network response in syncingTaskIds
        val candidateTasks = matchingTasks.filter { (task, _) ->
            task.id !in syncingTaskIds
        }

        if (candidateTasks.isEmpty()) return

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

        val (taskToSync, detectionToSync) = candidateTasks.first()
        syncingTaskIds.add(taskToSync.id)

        viewModelScope.launch {
            try {
                val dbHelper = CapturedItemDbHelper(appContext)

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

                val confidenceVal = if (detectionToSync.score <= 1.0f) detectionToSync.score * 100.0 else detectionToSync.score.toDouble()

                // Send complete_task request to server on every detection
                val result = ApiClient.completeTask(
                    token = token,
                    objectNameEn = taskToSync.label,
                    confidence = confidenceVal
                )

                if (result.statusCode == 401) {
                    sessionManager.clearSession()
                    _sessionExpiredEvent.value = true
                    return@launch
                }

                if (result.isSuccess && result.data != null) {
                    val completeData = result.data
                    val serverScore = completeData.score
                    val serverTasks = completeData.completedTasks

                    val serverTaskIds = serverTasks.mapNotNull { serverTask ->
                        CocoTasks.TASK_BY_LABEL[serverTask.objectNameEn.trim().lowercase()]?.id
                    }.toSet()

                    // Delete old captured image and record for taskToSync.id if previously completed
                    val oldItem = dbHelper.getItemByTaskId(taskToSync.id)
                    if (oldItem != null) {
                        try {
                            val oldFile = File(oldItem.imagePath)
                            if (oldFile.exists()) oldFile.delete()
                        } catch (e: Exception) {
                            Log.e("GameViewModel", "Error deleting old captured image file", e)
                        }
                        dbHelper.deleteItemByTaskId(taskToSync.id)
                    }

                    // Reconcile local items and image files with server tasks list
                    val localItems = dbHelper.getAllItems()
                    for (item in localItems) {
                        if (item.taskId !in serverTaskIds && item.taskId != taskToSync.id) {
                            try {
                                val file = File(item.imagePath)
                                if (file.exists()) file.delete()
                            } catch (e: Exception) {
                                Log.e("GameViewModel", "Error deleting local orphaned image file", e)
                            }
                        }
                    }
                    dbHelper.deleteItemsNotIn(serverTaskIds + taskToSync.id)

                    // SERVER SUCCESS! Complete task locally and save DB item
                    repository.completeTask(taskToSync.id)
                    dbHelper.insertItem(newItem)

                    val updatedCompletedSet = serverTaskIds + taskToSync.id
                    repository.replaceCompletedTasksAndScore(updatedCompletedSet, serverScore)
                    _gameProgress.value = repository.getProgress()

                    // Refresh mini leaderboard after task completion
                    fetchMiniLeaderboard(appContext)

                    Log.d("GameViewModel", "Successfully completed/updated task ${taskToSync.id} on server. Server score: $serverScore")
                } else {
                    // SERVER FAILED! Delete temp image, DO NOT mark completed locally
                    if (tempImagePath.isNotEmpty()) {
                        try {
                            File(tempImagePath).delete()
                        } catch (e: Exception) {
                            Log.e("GameViewModel", "Error deleting temp file on failure", e)
                        }
                    }
                    val errorMsg = result.message ?: "فشلت عملية تسجيل المهمة بالسيرفر"
                    Log.e("GameViewModel", "Server completeTask failed for ${taskToSync.id}: $errorMsg")
                    _inferenceError.value = errorMsg
                    triggerServerErrorSnackbar("تعذر الاتصال بالسيرفر لتسجيل المهمة: $errorMsg")
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "Exception during completeTask with server", e)
                _inferenceError.value = e.localizedMessage ?: "خطأ في الاتصال بالسيرفر"
                triggerServerErrorSnackbar("تعذر الاتصال بالسيرفر. يرجى المحاولة مرة أخرى لاحقاً.")
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
                if (result.statusCode == 401) {
                    sessionManager.clearSession()
                    _sessionExpiredEvent.value = true
                    return@launch
                }

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

                    // Refresh mini leaderboard after reset
                    fetchMiniLeaderboard(ctx)

                    Log.d("GameViewModel", "Game restarted on server. Server score reset to: ${result.data}")
                } else {
                    Log.e("GameViewModel", "Server restart failed: ${result.message}")
                    _inferenceError.value = result.message ?: "فشلت إعادة البداية من السيرفر"
                    triggerServerErrorSnackbar(result.message ?: "فشلت إعادة البداية بالسيرفر")
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error calling restart endpoint", e)
                _inferenceError.value = e.localizedMessage ?: "فشل الاتصال بالسيرفر لإعادة البداية"
                triggerServerErrorSnackbar("تعذر الاتصال بالسيرفر لإعادة البداية")
            }
        }
    }

    fun completeTask(taskId: String) {
        // Unused directly; tasks are completed via server API in onDetectionsReceived
    }
}
