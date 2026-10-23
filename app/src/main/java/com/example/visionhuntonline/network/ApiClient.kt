package com.example.visionhuntonline.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class ApiResult<T>(
    val isSuccess: Boolean,
    val data: T? = null,
    val message: String? = null,
    val statusCode: Int = 200
)

data class AuthData(
    val token: String,
    val username: String,
    val phone: String? = null,
    val score: Int = 0
)

data class ServerTaskItem(
    val id: Long,
    val objectNameAr: String,
    val objectNameEn: String,
    val rarity: String,
    val points: Int,
    val confidence: Double
)

data class CompleteTaskData(
    val username: String,
    val score: Int,
    val completedTasksCount: Int,
    val completedTasks: List<ServerTaskItem>,
    val message: String
)

data class AccountCheckData(
    val id: Long,
    val username: String,
    val phone: String?,
    val score: Int,
    val completedTasks: List<ServerTaskItem>,
    val completedTasksCount: Int
)

data class CurrentEventData(
    val id: Long,
    val title: String,
    val description: String?,
    val startsAt: String?,
    val endsAt: String?
)

data class HealthData(
    val status: String,
    val serverTime: String?
)

data class LeaderboardPlayer(
    val rank: Int,
    val username: String,
    val score: Int,
    val updatedAt: String? = null
)

data class LeaderboardData(
    val players: List<LeaderboardPlayer>,
    val totalPlayers: Int,
    val page: Int,
    val totalPages: Int
)

object ApiClient {
    var baseUrl: String = "http://192.168.1.6/visionhunt-web/public/api/v1/"

    suspend fun checkHealth(): ApiResult<HealthData> {
        return withContext(Dispatchers.IO) {
            try {
                val (responseCode, responseText) = executeGet("health.php", null)
                val json = parseJsonResponse(responseText, responseCode)

                val isSuccess = json.optBoolean("success")
                val dataObj = json.optJSONObject("data")
                val status = dataObj?.optString("status") ?: if (isSuccess) "ok" else "unavailable"

                if (responseCode == 200 && isSuccess && status == "ok") {
                    val healthData = HealthData(
                        status = status,
                        serverTime = dataObj?.optString("server_time")
                    )
                    ApiResult(true, healthData, "السيرفر يعمل بشكل طبيعي", responseCode)
                } else {
                    ApiResult(false, null, "السيرفر غير متاح حالياً ($responseCode)", responseCode)
                }
            } catch (e: Exception) {
                ApiResult(false, null, e.localizedMessage ?: "تعذر الاتصال بالسيرفر", 503)
            }
        }
    }

    suspend fun register(username: String, password: String, phone: String): ApiResult<AuthData> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().apply {
                    put("username", username)
                    put("password", password)
                    if (phone.isNotEmpty()) put("phone", phone)
                }
                val (responseCode, responseText) = executePost("register.php", jsonBody.toString(), null)
                val json = parseJsonResponse(responseText, responseCode)

                if (responseCode == 201 || responseCode in 200..299 || json.optBoolean("success")) {
                    val dataObj = json.optJSONObject("data")
                    val token = dataObj?.optString("token") ?: json.optString("token")
                    val score = dataObj?.optInt("score", 0) ?: 0
                    val msg = json.optString("message", "تم إنشاء الحساب بنجاح")
                    ApiResult(true, AuthData(token = token, username = username, phone = phone, score = score), msg, responseCode)
                } else {
                    val errorMsg = when (responseCode) {
                        409 -> "اسم المستخدم مسجل مسبقاً، اختر اسماً آخر"
                        429 -> "تم تجاوز عدد المحاولات المسموح بها. يرجى الانتظار قليلاً"
                        else -> json.optString("message").ifEmpty { "فشل إنشاء الحساب ($responseCode)" }
                    }
                    ApiResult(false, null, errorMsg, responseCode)
                }
            } catch (e: Exception) {
                ApiResult(false, null, e.localizedMessage ?: "خطأ في الاتصال بالسيرفر", 500)
            }
        }
    }

    suspend fun login(username: String, password: String): ApiResult<AuthData> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().apply {
                    put("username", username)
                    put("password", password)
                }
                val (responseCode, responseText) = executePost("login.php", jsonBody.toString(), null)
                val json = parseJsonResponse(responseText, responseCode)

                if (responseCode in 200..299 || json.optBoolean("success")) {
                    val dataObj = json.optJSONObject("data")
                    val playerObj = dataObj?.optJSONObject("player")

                    val token = dataObj?.optString("token") ?: json.optString("token")
                    val phone = playerObj?.optString("phone") ?: dataObj?.optString("phone") ?: ""
                    val score = playerObj?.optInt("score") ?: dataObj?.optInt("score") ?: 0
                    val msg = json.optString("message", "تم تسجيل الدخول بنجاح")

                    if (token.isNotEmpty()) {
                        ApiResult(true, AuthData(token = token, username = username, phone = phone, score = score), msg, responseCode)
                    } else {
                        ApiResult(false, null, "لم يتم العثور على رمز الدخول في الاستجابة", responseCode)
                    }
                } else {
                    val errorMsg = when (responseCode) {
                        401 -> "اسم المستخدم أو كلمة المرور غير صحيحة"
                        429 -> "تم تجاوز عدد المحاولات المسموح بها. يرجى الانتظار قليلاً"
                        else -> json.optString("message").ifEmpty { "فشل تسجيل الدخول ($responseCode)" }
                    }
                    ApiResult(false, null, errorMsg, responseCode)
                }
            } catch (e: Exception) {
                ApiResult(false, null, e.localizedMessage ?: "خطأ في الاتصال بالسيرفر", 500)
            }
        }
    }

    suspend fun updateProfile(
        token: String,
        oldPassword: String,
        phone: String?,
        newPassword: String?
    ): ApiResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().apply {
                    put("old_password", oldPassword)
                    if (!phone.isNullOrBlank()) put("phone", phone) else put("phone", "")
                    if (!newPassword.isNullOrBlank()) put("new_password", newPassword)
                }
                val (responseCode, responseText) = executePost("profile.php", jsonBody.toString(), token)
                val json = parseJsonResponse(responseText, responseCode)

                if (responseCode in 200..299 || json.optBoolean("success")) {
                    val msg = json.optString("message", "تم تحديث البيانات بنجاح")
                    ApiResult(true, msg, msg, responseCode)
                } else {
                    val errorMsg = when (responseCode) {
                        403 -> "كلمة المرور القديمة غير صحيحة"
                        401 -> "انتهت صلاحية الجلسة، يرجى إعادة تسجيل الدخول"
                        else -> json.optString("message").ifEmpty { "فشل تحديث البيانات ($responseCode)" }
                    }
                    ApiResult(false, null, errorMsg, responseCode)
                }
            } catch (e: Exception) {
                ApiResult(false, null, e.localizedMessage ?: "خطأ في الاتصال بالسيرفر", 500)
            }
        }
    }

    suspend fun logout(token: String): ApiResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val (responseCode, responseText) = executePost("logout.php", "{}", token)
                val json = parseJsonResponse(responseText, responseCode)
                if (responseCode in 200..299 || json.optBoolean("success")) {
                    val msg = json.optString("message", "تم تسجيل الخروج بنجاح")
                    ApiResult(true, msg, msg, responseCode)
                } else {
                    val errorMsg = json.optString("message")
                        .ifEmpty { json.optString("error", "فشل تسجيل الخروج من السيرفر ($responseCode)") }
                    ApiResult(false, null, errorMsg, responseCode)
                }
            } catch (e: Exception) {
                ApiResult(false, null, e.localizedMessage ?: "فشل الاتصال بالسيرفر لتسجيل الخروج", 500)
            }
        }
    }

    suspend fun completeTask(
        token: String,
        objectNameEn: String,
        confidence: Double
    ): ApiResult<CompleteTaskData> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().apply {
                    val taskObj = JSONObject().apply {
                        put("object_name_en", objectNameEn)
                        put("confidence", confidence)
                    }
                    put("completed_task", taskObj)
                }

                val (responseCode, responseText) = executePost("complete_task.php", jsonBody.toString(), token)
                val json = parseJsonResponse(responseText, responseCode)

                if (responseCode in 200..299 || json.optBoolean("success")) {
                    val dataObj = json.optJSONObject("data") ?: JSONObject()
                    val username = dataObj.optString("username")
                    val score = dataObj.optInt("score", 0)
                    val count = dataObj.optInt("completed_tasks_count", 0)

                    val tasksArray = dataObj.optJSONArray("completed_tasks") ?: org.json.JSONArray()
                    val tasksList = mutableListOf<ServerTaskItem>()

                    for (i in 0 until tasksArray.length()) {
                        val taskJson = tasksArray.optJSONObject(i) ?: continue
                        val confStr = taskJson.optString("confidence", "0")
                        val confVal = confStr.toDoubleOrNull() ?: taskJson.optDouble("confidence", 0.0)

                        tasksList.add(
                            ServerTaskItem(
                                id = taskJson.optLong("id"),
                                objectNameAr = taskJson.optString("object_name_ar"),
                                objectNameEn = taskJson.optString("object_name_en"),
                                rarity = taskJson.optString("rarity"),
                                points = taskJson.optInt("points"),
                                confidence = confVal
                            )
                        )
                    }

                    val completeTaskData = CompleteTaskData(
                        username = username,
                        score = score,
                        completedTasksCount = count,
                        completedTasks = tasksList,
                        message = json.optString("message", "تم تسجيل المهمة وتحديث النقاط بنجاح.")
                    )

                    ApiResult(true, completeTaskData, completeTaskData.message, responseCode)
                } else {
                    val errorMsg = when (responseCode) {
                        409 -> "لا يوجد حدث جارٍ حالياً يقبل تسجيل المهام"
                        401 -> "انتهت صلاحية الجلسة، يرجى إعادة تسجيل الدخول"
                        else -> json.optString("message").ifEmpty { "فشل تسجيل المهمة ($responseCode)" }
                    }
                    ApiResult(false, null, errorMsg, responseCode)
                }
            } catch (e: Exception) {
                ApiResult(false, null, e.localizedMessage ?: "خطأ أثناء تسجيل المهمة بالسيرفر", 500)
            }
        }
    }

    suspend fun restart(token: String): ApiResult<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val (responseCode, responseText) = executePost("restart.php", "{}", token)
                val json = parseJsonResponse(responseText, responseCode)

                if (responseCode in 200..299 || json.optBoolean("success")) {
                    val dataObj = json.optJSONObject("data")
                    val score = dataObj?.optInt("score") ?: 0
                    val msg = json.optString("message", "تمت إعادة تعيين نقاط اللاعب وقائمة المهام بنجاح.")
                    ApiResult(true, score, msg, responseCode)
                } else {
                    val errorMsg = when (responseCode) {
                        401 -> "انتهت صلاحية الجلسة، يرجى إعادة تسجيل الدخول"
                        else -> json.optString("message").ifEmpty { "فشلت إعادة البداية ($responseCode)" }
                    }
                    ApiResult(false, null, errorMsg, responseCode)
                }
            } catch (e: Exception) {
                ApiResult(false, null, e.localizedMessage ?: "خطأ أثناء إعادة البداية", 500)
            }
        }
    }

    suspend fun fetchPlayerStatus(token: String): ApiResult<AccountCheckData> {
        return withContext(Dispatchers.IO) {
            try {
                val (responseCode, responseText) = executeGet("player_status.php", token)
                val json = parseJsonResponse(responseText, responseCode)

                if (responseCode in 200..299 || json.optBoolean("success")) {
                    val dataObj = json.optJSONObject("data") ?: JSONObject()
                    val id = dataObj.optLong("id")
                    val username = dataObj.optString("username")
                    val phone = dataObj.optString("phone").takeIf { it.isNotEmpty() }
                    val score = dataObj.optInt("score", 0)
                    val count = dataObj.optInt("completed_tasks_count", 0)

                    val tasksArray = dataObj.optJSONArray("completed_tasks") ?: org.json.JSONArray()
                    val tasksList = mutableListOf<ServerTaskItem>()

                    for (i in 0 until tasksArray.length()) {
                        val taskJson = tasksArray.optJSONObject(i) ?: continue
                        val confidenceStr = taskJson.optString("confidence", "0")
                        val confidenceVal = confidenceStr.toDoubleOrNull() ?: taskJson.optDouble("confidence", 0.0)

                        tasksList.add(
                            ServerTaskItem(
                                id = taskJson.optLong("id"),
                                objectNameAr = taskJson.optString("object_name_ar"),
                                objectNameEn = taskJson.optString("object_name_en"),
                                rarity = taskJson.optString("rarity"),
                                points = taskJson.optInt("points"),
                                confidence = confidenceVal
                            )
                        )
                    }

                    val accountData = AccountCheckData(
                        id = id,
                        username = username,
                        phone = phone,
                        score = score,
                        completedTasks = tasksList,
                        completedTasksCount = count
                    )

                    val msg = json.optString("message", "تم استرجاع حالة اللاعب بنجاح")
                    ApiResult(true, accountData, msg, responseCode)
                } else {
                    val errorMsg = when (responseCode) {
                        401 -> "انتهت صلاحية الجلسة، يرجى إعادة تسجيل الدخول"
                        else -> json.optString("message").ifEmpty { "فشل استرجاع حالة اللاعب ($responseCode)" }
                    }
                    ApiResult(false, null, errorMsg, responseCode)
                }
            } catch (e: Exception) {
                ApiResult(false, null, e.localizedMessage ?: "خطأ أثناء الاتصال بالسيرفر", 500)
            }
        }
    }

    suspend fun checkAccount(token: String): ApiResult<AccountCheckData> = fetchPlayerStatus(token)

    suspend fun getCurrentEvent(token: String? = null): ApiResult<CurrentEventData?> {
        return withContext(Dispatchers.IO) {
            try {
                val (responseCode, responseText) = executeGet("current_event.php", token)
                val json = parseJsonResponse(responseText, responseCode)

                val isSuccess = json.optBoolean("success")
                val dataObj = json.optJSONObject("data")

                if (isSuccess && dataObj != null) {
                    val eventData = CurrentEventData(
                        id = dataObj.optLong("id"),
                        title = dataObj.optString("title"),
                        description = dataObj.optString("description").takeIf { it.isNotEmpty() },
                        startsAt = dataObj.optString("starts_at").takeIf { it.isNotEmpty() },
                        endsAt = dataObj.optString("ends_at").takeIf { it.isNotEmpty() }
                    )
                    val msg = json.optString("message", "حدث جارٍ")
                    ApiResult(true, eventData, msg, responseCode)
                } else {
                    val msg = json.optString("message", "لا يوجد حدث جارٍ حاليًا.")
                    ApiResult(true, null, msg, responseCode)
                }
            } catch (e: Exception) {
                ApiResult(false, null, e.localizedMessage ?: "خطأ أثناء جلب الحدث الحالي", 500)
            }
        }
    }

    suspend fun getLeaderboard(page: Int = 1): ApiResult<LeaderboardData> {
        return withContext(Dispatchers.IO) {
            try {
                val (responseCode, responseText) = executeGet("leaderboard.php?page=$page", null)
                val json = parseJsonResponse(responseText, responseCode)

                if (responseCode in 200..299 || json.optBoolean("success")) {
                    val dataObj = json.optJSONObject("data") ?: JSONObject()
                    val playersArray = dataObj.optJSONArray("players") ?: org.json.JSONArray()
                    val playersList = mutableListOf<LeaderboardPlayer>()

                    for (i in 0 until playersArray.length()) {
                        val playerJson = playersArray.optJSONObject(i) ?: continue
                        playersList.add(
                            LeaderboardPlayer(
                                rank = playerJson.optInt("rank", i + 1),
                                username = playerJson.optString("username"),
                                score = playerJson.optInt("score", 0),
                                updatedAt = playerJson.optString("updated_at")
                            )
                        )
                    }

                    val paginationObj = dataObj.optJSONObject("pagination")
                    val totalPlayers = paginationObj?.optInt("total_players") ?: playersList.size
                    val currentPage = paginationObj?.optInt("page") ?: page
                    val totalPages = paginationObj?.optInt("total_pages") ?: 1

                    val leaderboardData = LeaderboardData(
                        players = playersList,
                        totalPlayers = totalPlayers,
                        page = currentPage,
                        totalPages = totalPages
                    )

                    val msg = json.optString("message", "تم جلب الترتيب بنجاح")
                    ApiResult(true, leaderboardData, msg, responseCode)
                } else {
                    val errorMsg = json.optString("message").ifEmpty { "فشل جلب الترتيب ($responseCode)" }
                    ApiResult(false, null, errorMsg, responseCode)
                }
            } catch (e: Exception) {
                ApiResult(false, null, e.localizedMessage ?: "خطأ أثناء جلب الترتيب", 500)
            }
        }
    }

    private fun executePost(endpoint: String, jsonString: String, token: String?): Pair<Int, String> {
        val fullUrl = if (baseUrl.endsWith("/")) baseUrl + endpoint else "$baseUrl/$endpoint"
        val url = URL(fullUrl)

        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 10000
        conn.readTimeout = 10000

        if (!token.isNullOrEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer $token")
        }

        conn.doOutput = true
        conn.doInput = true

        OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
            writer.write(jsonString)
            writer.flush()
        }

        val responseCode = conn.responseCode
        val inputStream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
        val responseText = inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""

        conn.disconnect()
        return Pair(responseCode, responseText)
    }

    private fun executeGet(endpoint: String, token: String?): Pair<Int, String> {
        val fullUrl = if (baseUrl.endsWith("/")) baseUrl + endpoint else "$baseUrl/$endpoint"
        val url = URL(fullUrl)

        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 10000
        conn.readTimeout = 10000

        if (!token.isNullOrEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer $token")
        }

        conn.doInput = true

        val responseCode = conn.responseCode
        val inputStream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
        val responseText = inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""

        conn.disconnect()
        return Pair(responseCode, responseText)
    }

    private fun parseJsonResponse(responseText: String, responseCode: Int): JSONObject {
        val trimmed = responseText.trim()
        if (trimmed.isEmpty()) return JSONObject()

        // 1. Try parsing directly
        try {
            return JSONObject(trimmed)
        } catch (_: Exception) {}

        // 2. Try extracting JSON object if HTML/PHP warnings prepended/appended
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace > firstBrace) {
            val jsonSubstring = trimmed.substring(firstBrace, lastBrace + 1)
            try {
                return JSONObject(jsonSubstring)
            } catch (_: Exception) {}
        }

        // 3. Clear informative error based on response code and content
        val errorDetail = when {
            responseCode == 401 ->
                "انتهت صلاحية الجلسة، يرجى إعادة تسجيل الدخول (401)"
            responseCode == 503 ->
                "السيرفر غير متاح حالياً (503 Service Unavailable). تحقق من قاعدة البيانات والسيرفر."
            responseCode == 404 ->
                "الملف غير موجود على السيرفر (404 Not Found).\nتأكد من وجود الملف داخل C:\\xampp\\htdocs\\visionhunt-web\\public\\api\\v1\\"
            responseCode == 500 ->
                "خطأ برمجي داخلي في سيرفر PHP (500 Internal Server Error).\nتحقق من سجل أخطاء PHP في XAMPP."
            trimmed.startsWith("<!DOCTYPE", ignoreCase = true) || trimmed.startsWith("<html", ignoreCase = true) ->
                "السيرفر أرجع صفحة HTML بدلاً من JSON (كود الاستجابة: $responseCode).\nتأكد من مسار الـ API ورابط السيرفر."
            else ->
                "استجابة غير صالحة من السيرفر (كود الاستجابة: $responseCode)."
        }
        throw IllegalStateException(errorDetail)
    }
}
