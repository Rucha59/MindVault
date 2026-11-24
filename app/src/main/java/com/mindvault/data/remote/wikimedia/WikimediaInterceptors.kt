package com.mindvault.data.remote.wikimedia

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/** Wikimedia requires a descriptive User-Agent with contact information on every request. */
internal class UserAgentInterceptor(private val userAgent: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(chain.request().newBuilder().header("User-Agent", userAgent).build())
}

/**
 * When to retry a throttled request and when to stop sending requests for a while. Pure logic with an
 * injectable clock so it can be unit tested.
 */
internal class BackoffPolicy(
    private val now: () -> Long = { System.currentTimeMillis() },
    private val maxRetries: Int = 2,
    private val maxWaitMillis: Long = 3_000L,
    private val defaultCooldownMillis: Long = 30_000L,
) {
    @Volatile
    private var cooldownUntil = 0L

    /** True while a recent "too many requests" answer asks us to stay quiet. */
    fun isCoolingDown(): Boolean = now() < cooldownUntil

    /**
     * How long to wait before retry number [attempt] (1 = first retry), or null to give up. The server's
     * Retry-After wins over the default 1 s, 2 s steps; a wait longer than [maxWaitMillis] is not worth blocking for.
     */
    fun waitBeforeRetry(attempt: Int, retryAfterSeconds: Long?): Long? {
        if (attempt > maxRetries) return null
        val wait = retryAfterSeconds?.times(1000L) ?: (1000L shl (attempt - 1))
        return wait.takeIf { it <= maxWaitMillis }
    }

    fun startCooldown(retryAfterSeconds: Long?) {
        val wait = (retryAfterSeconds?.times(1000L) ?: defaultCooldownMillis)
            .coerceIn(MIN_COOLDOWN_MILLIS, MAX_COOLDOWN_MILLIS)
        cooldownUntil = now() + wait
    }

    private companion object {
        const val MIN_COOLDOWN_MILLIS = 5_000L
        const val MAX_COOLDOWN_MILLIS = 120_000L
    }
}

/**
 * Backs off on HTTP 429 (rate limited) and 503 (busy): waits and retries a couple of times, then gives the
 * response back. After a final 429 it answers further requests with a synthetic 429 for a short cooldown instead
 * of hitting the server again.
 */
internal class BackoffInterceptor(
    private val policy: BackoffPolicy = BackoffPolicy(),
    private val sleep: (Long) -> Unit = { Thread.sleep(it) },
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (policy.isCoolingDown()) return coolingDownResponse(request)

        var attempt = 0
        while (true) {
            val response = chain.proceed(request)
            if (response.code != 429 && response.code != 503) return response

            val retryAfter = response.header("Retry-After")?.trim()?.toLongOrNull()
            attempt++
            val wait = policy.waitBeforeRetry(attempt, retryAfter)
            if (wait == null) {
                if (response.code == 429) policy.startCooldown(retryAfter)
                return response
            }
            response.close()
            sleep(wait)
            if (chain.call().isCanceled()) throw IOException("Canceled")
        }
    }

    private fun coolingDownResponse(request: Request): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(429)
            .message("Too Many Requests (cooling down)")
            .body("".toResponseBody(null))
            .build()
}