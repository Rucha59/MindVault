package com.mindvault.data.remote.wikimedia

import java.lang.reflect.Proxy
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackoffTest {

    private var clock = 0L

    // ---------------------------------------------------------------- BackoffPolicy

    @Test
    fun waitsOneThenTwoSeconds_thenGivesUp() {
        val policy = BackoffPolicy(now = { clock })

        assertEquals(1_000L, policy.waitBeforeRetry(1, null))
        assertEquals(2_000L, policy.waitBeforeRetry(2, null))
        assertNull(policy.waitBeforeRetry(3, null))
    }

    @Test
    fun retryAfterWinsOverTheDefaultSteps_whenItIsShortEnough() {
        val policy = BackoffPolicy(now = { clock })

        assertEquals(2_000L, policy.waitBeforeRetry(1, 2L))
        assertNull(policy.waitBeforeRetry(1, 10L)) // too long to block a request thread for
    }

    @Test
    fun cooldown_lastsAsLongAsTheServerAsks_andThenEnds() {
        val policy = BackoffPolicy(now = { clock })
        assertFalse(policy.isCoolingDown())

        policy.startCooldown(retryAfterSeconds = 60)
        clock = 59_999
        assertTrue(policy.isCoolingDown())
        clock = 60_000
        assertFalse(policy.isCoolingDown())
    }

    @Test
    fun cooldown_usesADefaultWhenThereIsNoRetryAfter_andIsCapped() {
        val policy = BackoffPolicy(now = { clock })

        policy.startCooldown(null)
        clock = 29_999
        assertTrue(policy.isCoolingDown())
        clock = 30_000
        assertFalse(policy.isCoolingDown())

        clock = 0
        policy.startCooldown(retryAfterSeconds = 99_999)
        clock = 119_999
        assertTrue(policy.isCoolingDown())
        clock = 120_000
        assertFalse(policy.isCoolingDown())
    }

    // ---------------------------------------------------------------- BackoffInterceptor

    private val request = Request.Builder().url("https://en.wikipedia.org/x").build()

    private fun response(code: Int, retryAfter: String? = null): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("m")
            .apply { if (retryAfter != null) header("Retry-After", retryAfter) }
            .body("".toResponseBody(null))
            .build()

    /** A chain that answers each proceed() with the next queued response; counts how often it was asked. */
    private class Chains(private val answers: ArrayDeque<Response>) {
        var proceeded = 0

        fun create(request: Request): Interceptor.Chain {
            val call = Proxy.newProxyInstance(
                Call::class.java.classLoader, arrayOf(Call::class.java),
            ) { _, method, _ -> if (method.name == "isCanceled") false else null } as Call
            return Proxy.newProxyInstance(
                Interceptor.Chain::class.java.classLoader, arrayOf(Interceptor.Chain::class.java),
            ) { _, method, _ ->
                when (method.name) {
                    "request" -> request
                    "call" -> call
                    "proceed" -> { proceeded++; answers.removeFirst() }
                    else -> null
                }
            } as Interceptor.Chain
        }
    }

    private fun interceptor(policy: BackoffPolicy, sleeps: MutableList<Long>) =
        BackoffInterceptor(policy) { sleeps += it }

    @Test
    fun successfulResponse_isReturnedWithoutWaiting() {
        val sleeps = mutableListOf<Long>()
        val chains = Chains(ArrayDeque(listOf(response(200))))

        val result = interceptor(BackoffPolicy(now = { clock }), sleeps).intercept(chains.create(request))

        assertEquals(200, result.code)
        assertEquals(1, chains.proceeded)
        assertTrue(sleeps.isEmpty())
    }

    @Test
    fun rateLimitedOnce_isRetriedAfterOneSecond() {
        val sleeps = mutableListOf<Long>()
        val chains = Chains(ArrayDeque(listOf(response(429), response(200))))

        val result = interceptor(BackoffPolicy(now = { clock }), sleeps).intercept(chains.create(request))

        assertEquals(200, result.code)
        assertEquals(2, chains.proceeded)
        assertEquals(listOf(1_000L), sleeps)
    }

    @Test
    fun rateLimitedAgainAndAgain_givesUpAfterTwoRetries_thenStaysQuietDuringTheCooldown() {
        val sleeps = mutableListOf<Long>()
        val policy = BackoffPolicy(now = { clock })
        val chains = Chains(ArrayDeque(listOf(response(429), response(429), response(429))))
        val interceptor = interceptor(policy, sleeps)

        val first = interceptor.intercept(chains.create(request))

        assertEquals(429, first.code)
        assertEquals(3, chains.proceeded)
        assertEquals(listOf(1_000L, 2_000L), sleeps)

        // During the cooldown the server is not contacted at all.
        val second = interceptor.intercept(chains.create(request))
        assertEquals(429, second.code)
        assertEquals(3, chains.proceeded)
    }

    @Test
    fun longRetryAfter_isNotWaitedFor_butStartsTheCooldown() {
        val sleeps = mutableListOf<Long>()
        val policy = BackoffPolicy(now = { clock })
        val chains = Chains(ArrayDeque(listOf(response(429, retryAfter = "30"))))

        val result = interceptor(policy, sleeps).intercept(chains.create(request))

        assertEquals(429, result.code)
        assertTrue(sleeps.isEmpty())
        assertTrue(policy.isCoolingDown())
    }

    @Test
    fun serviceUnavailable_isRetried_butDoesNotStartACooldown() {
        val sleeps = mutableListOf<Long>()
        val policy = BackoffPolicy(now = { clock })
        val chains = Chains(ArrayDeque(listOf(response(503), response(503), response(503))))

        val result = interceptor(policy, sleeps).intercept(chains.create(request))

        assertEquals(503, result.code)
        assertEquals(3, chains.proceeded)
        assertFalse(policy.isCoolingDown())
    }
}