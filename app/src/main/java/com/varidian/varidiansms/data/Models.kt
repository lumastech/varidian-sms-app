package com.varidian.varidiansms.data

import org.json.JSONArray
import org.json.JSONObject

/** Account profile returned by the auth endpoints. */
data class UserInfo(
    val id: Long,
    val name: String,
    val email: String,
) {
    companion object {
        fun fromJson(json: JSONObject) = UserInfo(
            id = json.optLong("id"),
            name = json.optString("name"),
            email = json.optString("email"),
        )
    }
}

data class AuthSession(
    val apiKey: String,
    val user: UserInfo,
)

data class DashboardStats(
    val phonesTotal: Int,
    val phonesOnline: Int,
    val queued: Int,
    val sent: Int,
    val delivered: Int,
    val failed: Int,
    val received: Int,
    val sentToday: Int,
    val receivedToday: Int,
) {
    companion object {
        fun fromJson(json: JSONObject) = DashboardStats(
            phonesTotal = json.optInt("phones_total"),
            phonesOnline = json.optInt("phones_online"),
            queued = json.optInt("queued"),
            sent = json.optInt("sent"),
            delivered = json.optInt("delivered"),
            failed = json.optInt("failed"),
            received = json.optInt("received"),
            sentToday = json.optInt("sent_today"),
            receivedToday = json.optInt("received_today"),
        )
    }
}

data class PhoneItem(
    val id: String,
    val phoneNumber: String,
    val isOnline: Boolean,
    val lastHeartbeatAt: String?,
    val messagesPerMinute: Int,
    val maxSendAttempts: Int,
    val messageExpirationSeconds: Int,
    val scheduleActive: Boolean,
    val scheduleStart: String?,
    val scheduleEnd: String?,
    val scheduleTimezone: String?,
) {
    companion object {
        fun fromJson(json: JSONObject) = PhoneItem(
            id = json.optString("id"),
            phoneNumber = json.optString("phone_number"),
            isOnline = json.optBoolean("is_online"),
            lastHeartbeatAt = json.optStringOrNull("last_heartbeat_at"),
            messagesPerMinute = json.optInt("messages_per_minute", 6),
            maxSendAttempts = json.optInt("max_send_attempts", 2),
            messageExpirationSeconds = json.optInt("message_expiration_seconds", 900),
            scheduleActive = json.optBoolean("schedule_active"),
            scheduleStart = json.optStringOrNull("schedule_start"),
            scheduleEnd = json.optStringOrNull("schedule_end"),
            scheduleTimezone = json.optStringOrNull("schedule_timezone"),
        )
    }
}

data class MessageItem(
    val id: String,
    val owner: String,
    val contact: String,
    val content: String,
    val type: String,
    val status: String,
    val sim: String?,
    val failureReason: String?,
    val createdAt: String?,
) {
    val isOutgoing: Boolean get() = type == "mobile-terminated"

    companion object {
        fun fromJson(json: JSONObject) = MessageItem(
            id = json.optString("id"),
            owner = json.optString("owner"),
            contact = json.optString("contact"),
            content = json.optString("content"),
            type = json.optString("type"),
            status = json.optString("status"),
            sim = json.optStringOrNull("sim"),
            failureReason = json.optStringOrNull("failure_reason"),
            createdAt = json.optStringOrNull("created_at"),
        )
    }
}

data class WebhookItem(
    val id: String,
    val callbackUrl: String,
    val events: List<String>,
    val phoneNumbers: List<String>,
    val isActive: Boolean,
    val signingKey: String?, // present only in the create response
) {
    companion object {
        val ALL_EVENTS = listOf(
            "message.phone.received",
            "message.phone.sent",
            "message.phone.delivered",
            "message.send.failed",
            "message.send.expired",
            "message.call.missed",
            "phone.heartbeat.offline",
            "phone.heartbeat.online",
        )

        fun fromJson(json: JSONObject) = WebhookItem(
            id = json.optString("id"),
            callbackUrl = json.optString("callback_url"),
            events = json.optJSONArray("events").toStringList(),
            phoneNumbers = json.optJSONArray("phone_numbers").toStringList(),
            isActive = json.optBoolean("is_active", true),
            signingKey = json.optStringOrNull("signing_key"),
        )
    }
}

data class PhoneApiKeyItem(
    val id: String,
    val name: String,
    val keyPreview: String,
    val attachedPhoneNumber: String?,
    val lastUsedAt: String?,
    val plainKey: String?, // present only in the create response
) {
    companion object {
        fun fromJson(json: JSONObject) = PhoneApiKeyItem(
            id = json.optString("id"),
            name = json.optString("name"),
            keyPreview = json.optString("key_preview"),
            attachedPhoneNumber = json.optJSONObject("phone")?.optStringOrNull("phone_number"),
            lastUsedAt = json.optStringOrNull("last_used_at"),
            plainKey = json.optStringOrNull("api_key"),
        )
    }
}

data class DashboardData(
    val stats: DashboardStats,
    val phones: List<PhoneItem>,
    val recentMessages: List<MessageItem>,
)

// ------------------------------------------------------------------ helpers

fun JSONObject.optStringOrNull(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).map { optString(it) }
}

fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { i -> optJSONObject(i)?.let(transform) }
}
