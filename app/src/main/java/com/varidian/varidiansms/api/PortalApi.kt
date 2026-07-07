package com.varidian.varidiansms.api

import android.content.Context
import com.varidian.varidiansms.data.AuthSession
import com.varidian.varidiansms.data.DashboardData
import com.varidian.varidiansms.data.DashboardStats
import com.varidian.varidiansms.data.MessageItem
import com.varidian.varidiansms.data.PhoneApiKeyItem
import com.varidian.varidiansms.data.PhoneItem
import com.varidian.varidiansms.data.UserInfo
import com.varidian.varidiansms.data.WebhookItem
import com.varidian.varidiansms.data.mapObjects
import com.varidian.varidiansms.util.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int = 0) : ApiResult<Nothing>()
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Error -> this
}

/**
 * Coroutine client for the account ("portal") API — the /api/v1 routes,
 * with the master key (vsk_…) in the x-api-key header. The device gateway
 * API (/api/v1/phone) stays in the blocking [ApiClient] used by services.
 */
class PortalApi(context: Context) {

    private val prefs = AppPrefs(context.applicationContext)

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // ------------------------------------------------------------------ auth

    suspend fun login(email: String, password: String): ApiResult<AuthSession> =
        request(
            "POST", "/api/v1/auth/login",
            body = JSONObject()
                .put("email", email)
                .put("password", password)
                .put("device_name", deviceName()),
            authenticated = false,
        ).map(::parseAuthSession)

    suspend fun register(
        name: String,
        email: String,
        password: String,
        passwordConfirmation: String,
    ): ApiResult<AuthSession> =
        request(
            "POST", "/api/v1/auth/register",
            body = JSONObject()
                .put("name", name)
                .put("email", email)
                .put("password", password)
                .put("password_confirmation", passwordConfirmation)
                .put("device_name", deviceName()),
            authenticated = false,
        ).map(::parseAuthSession)

    suspend fun logout(): ApiResult<Unit> =
        request("POST", "/api/v1/auth/logout", body = JSONObject()).map { }

    // ------------------------------------------------------------- dashboard

    suspend fun dashboard(): ApiResult<DashboardData> =
        request("GET", "/api/v1/dashboard").map { json ->
            val data = json.getJSONObject("data")
            DashboardData(
                stats = DashboardStats.fromJson(data.getJSONObject("stats")),
                phones = data.optJSONArray("phones").mapObjects(PhoneItem::fromJson),
                recentMessages = data.optJSONArray("recent_messages").mapObjects(MessageItem::fromJson),
            )
        }

    // -------------------------------------------------------------- messages

    suspend fun messages(status: String? = null, type: String? = null): ApiResult<List<MessageItem>> {
        val query = buildString {
            append("?limit=100")
            if (!status.isNullOrEmpty()) append("&status=").append(status)
            if (!type.isNullOrEmpty()) append("&type=").append(type)
        }
        return request("GET", "/api/v1/messages$query").map { json ->
            json.getJSONObject("data").optJSONArray("data").mapObjects(MessageItem::fromJson)
        }
    }

    suspend fun sendMessage(from: String, to: String, content: String, sim: String = "DEFAULT"): ApiResult<MessageItem> =
        request(
            "POST", "/api/v1/messages/send",
            body = JSONObject()
                .put("from", from)
                .put("to", to)
                .put("content", content)
                .put("sim", sim),
        ).map { MessageItem.fromJson(it.getJSONObject("data")) }

    suspend fun bulkSendMessage(from: String, to: List<String>, content: String): ApiResult<Int> =
        request(
            "POST", "/api/v1/messages/bulk-send",
            body = JSONObject()
                .put("from", from)
                .put("to", JSONArray(to))
                .put("content", content),
        ).map { it.optJSONArray("data")?.length() ?: to.size }

    // ---------------------------------------------------------------- phones

    suspend fun phones(): ApiResult<List<PhoneItem>> =
        request("GET", "/api/v1/phones").map { json ->
            json.optJSONArray("data").mapObjects(PhoneItem::fromJson)
        }

    suspend fun updatePhone(id: String, fields: JSONObject): ApiResult<PhoneItem> =
        request("PUT", "/api/v1/phones/$id", body = fields)
            .map { PhoneItem.fromJson(it.getJSONObject("data")) }

    suspend fun deletePhone(id: String): ApiResult<Unit> =
        request("DELETE", "/api/v1/phones/$id").map { }

    // -------------------------------------------------------------- webhooks

    suspend fun webhooks(): ApiResult<List<WebhookItem>> =
        request("GET", "/api/v1/webhooks").map { json ->
            json.optJSONArray("data").mapObjects(WebhookItem::fromJson)
        }

    suspend fun createWebhook(url: String, events: List<String>, phoneNumbers: List<String>): ApiResult<WebhookItem> =
        request(
            "POST", "/api/v1/webhooks",
            body = JSONObject().apply {
                put("url", url)
                put("events", JSONArray(events))
                if (phoneNumbers.isNotEmpty()) put("phone_numbers", JSONArray(phoneNumbers))
            },
        ).map { WebhookItem.fromJson(it.getJSONObject("data")) }

    suspend fun updateWebhook(
        id: String,
        url: String? = null,
        events: List<String>? = null,
        isActive: Boolean? = null,
    ): ApiResult<WebhookItem> =
        request(
            "PUT", "/api/v1/webhooks/$id",
            body = JSONObject().apply {
                url?.let { put("url", it) }
                events?.let { put("events", JSONArray(it)) }
                isActive?.let { put("is_active", it) }
            },
        ).map { WebhookItem.fromJson(it.getJSONObject("data")) }

    suspend fun deleteWebhook(id: String): ApiResult<Unit> =
        request("DELETE", "/api/v1/webhooks/$id").map { }

    // -------------------------------------------------------- phone api keys

    suspend fun phoneApiKeys(): ApiResult<List<PhoneApiKeyItem>> =
        request("GET", "/api/v1/phone-api-keys").map { json ->
            json.optJSONArray("data").mapObjects(PhoneApiKeyItem::fromJson)
        }

    suspend fun createPhoneApiKey(name: String): ApiResult<PhoneApiKeyItem> =
        request("POST", "/api/v1/phone-api-keys", body = JSONObject().put("name", name))
            .map { PhoneApiKeyItem.fromJson(it.getJSONObject("data")) }

    suspend fun detachPhoneApiKey(id: String): ApiResult<Unit> =
        request("POST", "/api/v1/phone-api-keys/$id/detach-phone", body = JSONObject()).map { }

    suspend fun deletePhoneApiKey(id: String): ApiResult<Unit> =
        request("DELETE", "/api/v1/phone-api-keys/$id").map { }

    // ------------------------------------------------------------- internals

    private fun parseAuthSession(json: JSONObject): AuthSession {
        val data = json.getJSONObject("data")
        return AuthSession(
            apiKey = data.getString("api_key"),
            user = UserInfo.fromJson(data.getJSONObject("user")),
        )
    }

    private fun deviceName(): String =
        "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim().ifEmpty { "Android" }

    private suspend fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        authenticated: Boolean = true,
    ): ApiResult<JSONObject> = withContext(Dispatchers.IO) {
        val base = prefs.serverUrl.trim().trimEnd('/')

        val builder = Request.Builder()
            .url(base + path)
            .header("Accept", "application/json")

        if (authenticated) {
            val key = prefs.accountApiKey
            if (key.isEmpty()) return@withContext ApiResult.Error("Not logged in.", 401)
            builder.header("x-api-key", key)
        }

        when (method) {
            "GET" -> builder.get()
            "DELETE" -> builder.delete()
            else -> builder.method(method, (body ?: JSONObject()).toString().toRequestBody(JSON_TYPE))
        }

        try {
            http.newCall(builder.build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(text) }.getOrElse { JSONObject() }
                if (response.isSuccessful) {
                    ApiResult.Success(json)
                } else {
                    ApiResult.Error(extractErrorMessage(json, response.code), response.code)
                }
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Could not reach the server. Check your connection.")
        }
    }

    /** Pulls the friendliest message out of Laravel's various error shapes. */
    private fun extractErrorMessage(json: JSONObject, code: Int): String {
        json.optJSONObject("errors")?.let { errors ->
            errors.keys().asSequence().firstOrNull()?.let { field ->
                errors.optJSONArray(field)?.optString(0)?.takeIf { it.isNotEmpty() }?.let { return it }
            }
        }
        json.optJSONObject("data")?.let { data ->
            data.keys().asSequence().firstOrNull()?.let { field ->
                data.optJSONArray(field)?.optString(0)?.takeIf { it.isNotEmpty() }?.let { return it }
            }
        }
        json.optString("message").takeIf { it.isNotEmpty() }?.let { return it }
        return "Request failed (HTTP $code)."
    }

    companion object {
        private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
