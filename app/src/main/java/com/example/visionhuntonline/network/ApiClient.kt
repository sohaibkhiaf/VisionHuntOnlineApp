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
    val message: String? = null
)

data class AuthData(
    val token: String,
    val username: String,
    val phone: String? = null
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

object ApiClient {
    var baseUrl: String = "http://192.168.1.6/visionhunt-web/public/api/v1/"

    suspend fun register(username: String, password: String, phone: String): ApiResult<AuthData> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().apply {
                    put("username", username)
                    put("password", password)
                    put("phone", phone)
                }
                val (responseCode, responseText) = executePost("register.php", jsonBody.toString(), null)
                val json = parseJsonResponse(responseText, responseCode)

                if (responseCode in 200..299 || json.optString("status") == "success") {
                    val dataObj = json.optJSONObject("data")
                    val token = dataObj?.optString("token") ?: json.optString("token")
                    val msg = json.optString("message", "تم إنشاء الحساب بنجاح")
                    ApiResult(true, AuthData(token = token, username = username, phone = phone), msg)
                } else {
                    val errorMsg = json.optString("message")
                        .ifEmpty { json.optString("error", "فشل إنشاء الحساب ($responseCode)") }
                    ApiResult(false, null, errorMsg)
                }
            } catch (e: Exception) {
                ApiResult(false, null, e.localizedMessage ?: "خطأ في الاتصال بالسيرفر")
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

                if (responseCode in 200..299 || json.optString("status") == "success") {
                    val dataObj = json.optJSONObject("data")
                    val token = dataObj?.optString("token") ?: json.optString("token")
                    val phone = dataObj?.optString("phone") ?: ""
                    val msg = json.optString("message", "تم تسجيل الدخول بنجاح")
                    if (token.isNotEmpty()) {
                        ApiResult(true, AuthData(token = token, username = username, phone = phone), msg)
                    } else {
                        ApiResult(false, null, "لم يتم العثور على رمز الدخول في الاستجابة")
                    }
                } else {
                    val errorMsg = json.optString("message")
                        .ifEmpty { json.optString("error", "اسم المستخدم أو كلمة المرور غير صحيحة ($responseCode)") }
                    ApiResult(false, null, errorMsg)
                }
            } catch (e: Exception) {
                ApiResult(false, null, e.localizedMessage ?: "خطأ في الاتصال بالسيرفر")
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
                    if (!phone.isNullOrBlank()) put("phone", phone)
                    if (!newPassword.isNullOrBlank()) put("new_password", newPassword)
                }
                val (responseCode, responseText) = executePost("profile.php", jsonBody.toString(), token)
                val json = parseJsonResponse(responseText, responseCode)

                if (responseCode in 200..299 || json.optString("status") == "success") {
                    val msg = json.optString("message", "تم تحديث البيانات بنجاح")
                    ApiResult(true, msg, msg)
                } else {
                    val errorMsg = json.optString("message")
                        .ifEmpty { json.optString("error", "فشل تحديث البيانات ($responseCode)") }
                    ApiResult(false, null, errorMsg)
                }
            } catch (e: Exception) {
                ApiResult(false, null, e.localizedMessage ?: "خطأ في الاتصال بالسيرفر")
            }
        }
    }

    suspend fun logout(token: String): ApiResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val (responseCode, responseText) = executePost("logout.php", "{}", token)
                val json = parseJsonResponse(responseText, responseCode)
                if (responseCode in 200..299 || json.optBoolean("success") || json.optString("status") == "success") {
                    val msg = json.optString("message", "تم تسجيل الخروج بنجاح")
                    ApiResult(true, msg, msg)
                } else {
                    val errorMsg = json.optString("message")
                        .ifEmpty { json.optString("error", "فشل تسجيل الخروج من السيرفر ($responseCode)") }
                    ApiResult(false, null, errorMsg)
                }
            } catch (e: Exception) {
                ApiResult(false, null, e.localizedMessage ?: "فشل الاتصال بالسيرفر لتسجيل الخروج")
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

                if (responseCode in 200..299 || json.optBoolean("success") || json.optString("status") == "success") {
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

                    ApiResult(true, completeTaskData, completeTaskData.message)
                } else {
                    val errorMsg = json.optString("message")
                        .ifEmpty { json.optString("error", "فشل تسجيل المهمة ($responseCode)") }
                    ApiResult(false, null, errorMsg)
                }
            } catch (e: Exception) {
                ApiResult(false, null, e.localizedMessage ?: "خطأ أثناء تسجيل المهمة بالسيرفر")
            }
        }
    }

    suspend fun restart(token: String): ApiResult<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val (responseCode, responseText) = executePost("restart.php", "{}", token)
                val json = parseJsonResponse(responseText, responseCode)

                if (responseCode in 200..299 || json.optBoolean("success") || json.optString("status") == "success") {
                    val dataObj = json.optJSONObject("data")
                    val score = dataObj?.optInt("score") ?: 0
                    val msg = json.optString("message", "تمت إعادة تعيين نقاط اللاعب وقائمة المهام بنجاح.")
                    ApiResult(true, score, msg)
                } else {
                    val errorMsg = json.optString("message")
                        .ifEmpty { json.optString("error", "فشلت إعادة البداية ($responseCode)") }
                    ApiResult(false, null, errorMsg)
                }
            } catch (e: Exception) {
                ApiResult(false, null, e.localizedMessage ?: "خطأ أثناء إعادة البداية")
            }
        }
    }

    suspend fun checkAccount(token: String): ApiResult<AccountCheckData> {
        return withContext(Dispatchers.IO) {
            try {
                val (responseCode, responseText) = executeGet("check.php", token)
                val json = parseJsonResponse(responseText, responseCode)

                if (responseCode in 200..299 || json.optBoolean("success") || json.optString("status") == "success") {
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

                    val msg = json.optString("message", "تم استرجاع بيانات الحساب بنجاح")
                    ApiResult(true, accountData, msg)
                } else {
                    val errorMsg = json.optString("message")
                        .ifEmpty { json.optString("error", "فشل استرجاع بيانات الحساب ($responseCode)") }
                    ApiResult(false, null, errorMsg)
                }
            } catch (e: Exception) {
                ApiResult(false, null, e.localizedMessage ?: "خطأ أثناء الاتصال بالسيرفر")
            }
        }
    }

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
                    ApiResult(true, eventData, msg)
                } else {
                    val msg = json.optString("message", "لا يوجد حدث جارٍ حاليًا.")
                    ApiResult(true, null, msg)
                }
            } catch (e: Exception) {
                ApiResult(false, null, e.localizedMessage ?: "خطأ أثناء جلب الحدث الحالي")
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
