package com.varidian.varidiansms.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Parsing of the billing payloads served under `/api/v1/billing`. */
class BillingModelsTest {

    @Test
    fun `parses an active subscription`() {
        val subscription = SubscriptionInfo.fromJson(
            JSONObject(
                """
                {
                  "plan": "starter",
                  "plan_name": "Starter",
                  "status": "active",
                  "expires_at": "2026-08-12T00:00:00+02:00",
                  "price": 15000,
                  "currency": "ZMW",
                  "pending_plan": null
                }
                """.trimIndent(),
            ),
        )

        assertEquals("starter", subscription.planCode)
        assertEquals("Starter", subscription.planName)
        assertTrue(subscription.isActive)
        assertEquals(15000, subscription.price)
        assertNull(subscription.pendingPlan)
    }

    @Test
    fun `parses a queued downgrade and an expired status`() {
        val subscription = SubscriptionInfo.fromJson(
            JSONObject("""{"plan":"business","status":"expired","pending_plan":"starter","price":45000}"""),
        )

        assertFalse(subscription.isActive)
        assertEquals("starter", subscription.pendingPlan)
        // plan_name is absent — fall back to the code rather than showing blank.
        assertEquals("business", subscription.planName)
        assertEquals("ZMW", subscription.currency)
    }

    @Test
    fun `treats a null limit as unlimited rather than zero`() {
        val usage = UsageSummary.fromJson(
            JSONObject(
                """
                {
                  "period": "2026-07",
                  "messages": {"used": 1240, "limit": 5000, "remaining": 3760},
                  "phones": {"used": 1, "limit": 1},
                  "webhooks": {"used": 2, "limit": null},
                  "resets_at": "2026-08-01T00:00:00+02:00"
                }
                """.trimIndent(),
            ),
        )

        assertEquals(1240, usage.messages.used)
        assertEquals(5000, usage.messages.limit)
        assertFalse(usage.messages.isUnlimited)
        assertEquals(0.248f, usage.messages.fraction, 0.001f)

        assertTrue(usage.webhooks.isUnlimited)
        assertEquals(0f, usage.webhooks.fraction, 0f)

        // A plan at its cap fills the bar without overflowing it.
        assertEquals(1f, usage.phones.fraction, 0f)
    }

    @Test
    fun `usage fraction never exceeds one when over quota`() {
        val metric = UsageMetric(used = 6000, limit = 5000)

        assertEquals(1f, metric.fraction, 0f)
    }

    @Test
    fun `parses the wallet with its ledger`() {
        val wallet = WalletInfo.fromJson(
            JSONObject(
                """
                {
                  "balance": 32000,
                  "currency": "ZMW",
                  "transactions": [
                    {"id": 9, "type": "plan_charge", "amount": -15000, "balance_after": 32000, "reference": null, "created_at": "2026-07-12T09:14:02+02:00"},
                    {"id": 8, "type": "topup", "amount": 47000, "balance_after": 47000, "reference": "vsms-topup-8", "created_at": "2026-07-12T09:10:41+02:00"}
                  ]
                }
                """.trimIndent(),
            ),
        )

        assertEquals(32000, wallet.balance)
        assertEquals(2, wallet.transactions.size)
        assertEquals("plan_charge", wallet.transactions[0].type)
        assertEquals(-15000, wallet.transactions[0].amount)
        assertNull(wallet.transactions[0].reference)
        assertEquals("vsms-topup-8", wallet.transactions[1].reference)
    }

    @Test
    fun `formats ngwee as kwacha`() {
        assertEquals("ZMW 150.00", formatKwacha(15000))
        assertEquals("ZMW 0.00", formatKwacha(0))
        assertEquals("ZMW 1.05", formatKwacha(105))
        assertEquals("ZMW 12,345.67", formatKwacha(1234567))
    }
}
