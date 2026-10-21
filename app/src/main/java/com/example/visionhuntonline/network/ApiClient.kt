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
                val msg = json.optString("message", "تم تسجيل الخروج")
                ApiResult(true, msg, msg)
            } catch (e: Exception) {
                ApiResult(true, "تم تسجيل الخروج محلياً", "تم تسجيل الخروج")
            }
        }
    }

    suspend fun sync(
        token: String,
        score: Int
    ): ApiResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().apply {
                    put("score", score)
                }

                val (responseCode, responseText) = executePost("sync.php", jsonBody.toString(), token)
                val json = parseJsonResponse(responseText, responseCode)

                if (responseCode in 200..299 || json.optBoolean("success") || json.optString("status") == "success") {
                    val msg = json.optString("message", "تمت مزامنة نقاط اللاعب بنجاح.")
                    ApiResult(true, msg, msg)
                } else {
                    val errorMsg = json.optString("message")
                        .ifEmpty { json.optString("error", "فشلت المزامنة ($responseCode)") }
                    ApiResult(false, null, errorMsg)
                }
            } catch (e: Exception) {
                ApiResult(false, null, e.localizedMessage ?: "خطأ أثناء المزامنة")
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
