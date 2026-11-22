package com.mindvault.domain.scheduler

import com.mindvault.domain.model.ReviewRating
import com.mindvault.domain.model.ReviewState
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Sm2SchedulerTest {

    private val scheduler = Sm2Scheduler()
    private val zone = ZoneOffset.UTC
    private val now = ZonedDateTime.of(2026, 11, 18, 15, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun startOf(year: Int, month: Int, day: Int) =
        LocalDate.of(year, month, day).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun reviewed(interval: Int, ease: Double) = ReviewState(
        flashcardId = "c", lastReviewedAt = 1L, nextReviewAt = 1L,
        reviewCount = 3, easeFactor = ease, intervalDays = interval,
    )

    @Test
    fun newCardGoodIsDueInTwoDaysFromMidnight() {
        val next = scheduler.next(ReviewState.initial("c", now), ReviewRating.GOOD, now, zone)
        assertEquals(2, next.intervalDays)
        assertEquals(startOf(2026, 11, 20), next.nextReviewAt)
        assertEquals(1, next.reviewCount)
        assertEquals(now, next.lastReviewedAt)
        assertEquals(2.5, next.easeFactor, 0.0)
    }

    @Test
    fun newCardIntervalsPerRating() {
        val s = ReviewState.initial("c", now)
        assertEquals(1, scheduler.intervalDays(s, ReviewRating.AGAIN))
        assertEquals(1, scheduler.intervalDays(s, ReviewRating.HARD))
        assertEquals(2, scheduler.intervalDays(s, ReviewRating.GOOD))
        assertEquals(4, scheduler.intervalDays(s, ReviewRating.EASY))
    }

    @Test
    fun againResetsIntervalAndLowersEase() {
        val next = scheduler.next(reviewed(10, 2.5), ReviewRating.AGAIN, now, zone)
        assertEquals(1, next.intervalDays)
        assertEquals(2.3, next.easeFactor, 0.0)
        assertEquals(startOf(2026, 11, 19), next.nextReviewAt)
    }

    @Test
    fun laterReviewsMultiplyTheInterval() {
        val s = reviewed(20, 2.5)
        assertEquals(24, scheduler.intervalDays(s, ReviewRating.HARD))
        assertEquals(50, scheduler.intervalDays(s, ReviewRating.GOOD))
        assertEquals(65, scheduler.intervalDays(s, ReviewRating.EASY))
    }

    @Test
    fun easeChangesPerRating() {
        val s = reviewed(10, 2.5)
        assertEquals(2.35, scheduler.next(s, ReviewRating.HARD, now, zone).easeFactor, 0.0)
        assertEquals(2.5, scheduler.next(s, ReviewRating.GOOD, now, zone).easeFactor, 0.0)
        assertEquals(2.65, scheduler.next(s, ReviewRating.EASY, now, zone).easeFactor, 0.0)
    }

    @Test
    fun intervalsNeverShrinkBetweenHardGoodEasy() {
        for (interval in listOf(1, 2, 5, 30, 200)) {
            for (ease in listOf(1.3, 2.0, 2.5, 3.0)) {
                val s = reviewed(interval, ease)
                val hard = scheduler.intervalDays(s, ReviewRating.HARD)
                val good = scheduler.intervalDays(s, ReviewRating.GOOD)
                val easy = scheduler.intervalDays(s, ReviewRating.EASY)
                assertTrue(hard > interval || hard == Sm2Scheduler.MAX_INTERVAL_DAYS)
                assertTrue(good >= hard)
                assertTrue(easy >= good)
            }
        }
    }

    @Test
    fun easeStaysWithinLimits() {
        assertEquals(1.3, scheduler.next(reviewed(5, 1.3), ReviewRating.AGAIN, now, zone).easeFactor, 0.0)
        assertEquals(3.0, scheduler.next(reviewed(5, 3.0), ReviewRating.EASY, now, zone).easeFactor, 0.0)
    }

    @Test
    fun intervalIsCapped() {
        assertEquals(365, scheduler.intervalDays(reviewed(300, 3.0), ReviewRating.EASY))
    }
}