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

// ------------------------------------------------------------------ billing

/**
 * The account's current plan. Money is always integer ngwee on the wire
 * (1 ZMW = 100 ngwee) — use [formatKwacha] to display it.
 */
data class SubscriptionInfo(
    val planCode: String,
    val planName: String,
    val status: String,
    val expiresAt: String?,
    val price: Int,
    val currency: String,
    /** Set when a downgrade is queued to apply at [expiresAt]. */
    val pendingPlan: String?,
) {
    val isActive: Boolean get() = status == "active"

    companion object {
        fun fromJson(json: JSONObject) = SubscriptionInfo(
            planCode = json.optString("plan"),
            planName = json.optString("plan_name").ifEmpty { json.optString("plan") },
            status = json.optString("status"),
            expiresAt = json.optStringOrNull("expires_at"),
            price = json.optInt("price"),
            currency = json.optString("currency").ifEmpty { "ZMW" },
            pendingPlan = json.optStringOrNull("pending_plan"),
        )
    }
}

/** One metered resource. A null [limit] means the plan grants unlimited use. */
data class UsageMetric(
    val used: Int,
    val limit: Int?,
) {
    val isUnlimited: Boolean get() = limit == null

    /** 0f..1f for the progress bar; unlimited and zero-limit both read as empty. */
    val fraction: Float
        get() = if (limit == null || limit <= 0) 0f else (used.toFloat() / limit).coerceIn(0f, 1f)

    companion object {
        fun fromJson(json: JSONObject?) = UsageMetric(
            used = json?.optInt("used") ?: 0,
            limit = json?.optIntOrNull("limit"),
        )
    }
}

data class UsageSummary(
    val period: String,
    val messages: UsageMetric,
    val phones: UsageMetric,
    val webhooks: UsageMetric,
    val resetsAt: String?,
) {
    companion object {
        fun fromJson(json: JSONObject) = UsageSummary(
            period = json.optString("period"),
            messages = UsageMetric.fromJson(json.optJSONObject("messages")),
            phones = UsageMetric.fromJson(json.optJSONObject("phones")),
            webhooks = UsageMetric.fromJson(json.optJSONObject("webhooks")),
            resetsAt = json.optStringOrNull("resets_at"),
        )
    }
}

/** A wallet ledger entry. [amount] is signed ngwee — credits positive. */
data class WalletTransactionItem(
    val id: Long,
    val type: String,
    val amount: Int,
    val balanceAfter: Int,
    val reference: String?,
    val createdAt: String?,
) {
    companion object {
        fun fromJson(json: JSONObject) = WalletTransactionItem(
            id = json.optLong("id"),
            type = json.optString("type"),
            amount = json.optInt("amount"),
            balanceAfter = json.optInt("balance_after"),
            reference = json.optStringOrNull("reference"),
            createdAt = json.optStringOrNull("created_at"),
        )
    }
}

data class WalletInfo(
    val balance: Int,
    val currency: String,
    val transactions: List<WalletTransactionItem>,
) {
    companion object {
        fun fromJson(json: JSONObject) = WalletInfo(
            balance = json.optInt("balance"),
            currency = json.optString("currency").ifEmpty { "ZMW" },
            transactions = json.optJSONArray("transactions").mapObjects(WalletTransactionItem::fromJson),
        )
    }
}

/** Everything the billing screen shows, fetched in one pass. */
data class BillingData(
    val subscription: SubscriptionInfo,
    val usage: UsageSummary,
    val wallet: WalletInfo,
)

// ------------------------------------------------------------------ helpers

/**
 * Renders integer ngwee as "ZMW 150.00". The locale is pinned so amounts
 * read identically to the web portal on every device.
 */
fun formatKwacha(ngwee: Int, currency: String = "ZMW"): String =
    String.format(java.util.Locale.US, "%s %,.2f", currency, ngwee / 100.0)

fun JSONObject.optStringOrNull(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

/** Distinguishes an explicit JSON null (plan limit = "unlimited") from 0. */
fun JSONObject.optIntOrNull(key: String): Int? =
    if (!has(key) || isNull(key)) null else optInt(key)

fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).map { optString(it) }
}

fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { i -> optJSONObject(i)?.let(transform) }
}
