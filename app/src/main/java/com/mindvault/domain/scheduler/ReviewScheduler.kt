package com.mindvault.domain.scheduler

import com.mindvault.core.util.nowMillis
import com.mindvault.domain.model.ReviewRating
import com.mindvault.domain.model.ReviewState
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

/** Decides when a card comes back. The only place that knows the algorithm, so it can be swapped (see FSRS plan). */
interface ReviewScheduler {
    /** Days until the next review if [state] is answered with [rating]. Used for the button labels. */
    fun intervalDays(state: ReviewState, rating: ReviewRating): Int

    /** The state after [rating]. Due dates are calendar days in [zone]: the card is due from midnight. */
    fun next(
        state: ReviewState,
        rating: ReviewRating,
        now: Long = nowMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): ReviewState

    /** Days until a card returns when it came back after Again in the same session and is answered with [rating]. */
    fun relearnIntervalDays(rating: ReviewRating): Int

    /**
     * The state after a card that was marked Again is answered again in the same session. Only the interval and due
     * date change: ease and review count stay as the first rating left them, so one lapse is not penalised twice.
     */
    fun relearn(
        state: ReviewState,
        rating: ReviewRating,
        now: Long = nowMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): ReviewState
}

/** SM-2 with four ratings. Ease changes are applied after the interval is computed from the current ease. */
class Sm2Scheduler @Inject constructor() : ReviewScheduler {

    override fun intervalDays(state: ReviewState, rating: ReviewRating): Int {
        if (rating == ReviewRating.AGAIN) return 1
        val prev = state.intervalDays
        if (prev <= 0) {
            return when (rating) {
                ReviewRating.HARD -> 1
                ReviewRating.GOOD -> 2
                else -> 4
            }
        }
        val hard = max(prev + 1, (prev * HARD_MULTIPLIER).roundToInt())
        val good = max(hard, (prev * state.easeFactor).roundToInt())
        val easy = max(good + 1, (prev * state.easeFactor * EASY_BONUS).roundToInt())
        val days = when (rating) {
            ReviewRating.HARD -> hard
            ReviewRating.GOOD -> good
            else -> easy
        }
        return days.coerceAtMost(MAX_INTERVAL_DAYS)
    }

    override fun next(state: ReviewState, rating: ReviewRating, now: Long, zone: ZoneId): ReviewState {
        val interval = intervalDays(state, rating)
        return state.copy(
            lastReviewedAt = now,
            nextReviewAt = dueAt(now, zone, interval),
            reviewCount = state.reviewCount + 1,
            easeFactor = newEase(state.easeFactor, rating),
            intervalDays = interval,
        )
    }

    override fun relearnIntervalDays(rating: ReviewRating): Int = when (rating) {
        ReviewRating.AGAIN, ReviewRating.HARD -> 1
        ReviewRating.GOOD -> 2
        ReviewRating.EASY -> 3
    }

    override fun relearn(state: ReviewState, rating: ReviewRating, now: Long, zone: ZoneId): ReviewState {
        val interval = relearnIntervalDays(rating)
        return state.copy(
            lastReviewedAt = now,
            nextReviewAt = dueAt(now, zone, interval),
            intervalDays = interval,
        )
    }

    /** Midnight (start of day) [days] calendar days after [now], in [zone]. */
    private fun dueAt(now: Long, zone: ZoneId, days: Int): Long =
        Instant.ofEpochMilli(now).atZone(zone).toLocalDate().plusDays(days.toLong())
            .atStartOfDay(zone).toInstant().toEpochMilli()

    private fun newEase(ease: Double, rating: ReviewRating): Double {
        val delta = when (rating) {
            ReviewRating.AGAIN -> -0.20
            ReviewRating.HARD -> -0.15
            ReviewRating.GOOD -> 0.0
            ReviewRating.EASY -> 0.15
        }
        val updated = (ease + delta).coerceIn(MIN_EASE, MAX_EASE)
        return Math.round(updated * 100) / 100.0 // avoids 2.3000000000000003 drift
    }

    companion object {
        const val MIN_EASE = 1.3
        const val MAX_EASE = 3.0
        const val MAX_INTERVAL_DAYS = 365
        private const val HARD_MULTIPLIER = 1.2
        private const val EASY_BONUS = 1.3
    }
}