package com.mindvault.domain.usecase

import com.mindvault.domain.model.NotificationPlan
import com.mindvault.domain.model.NotificationSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPlannerTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 11, 27)
    private val now = at(today, 18, 0)

    private fun at(date: LocalDate, hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(date, java.time.LocalTime.of(hour, minute)).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun daysAgo(n: Long) = today.minusDays(n)

    private fun nudge(
        savedDaysAgo: Long?,
        settings: NotificationSettings = NotificationSettings(firstSeenEpochDay = daysAgo(30).toEpochDay()),
        lines: Int = 8,
    ) = NotificationPlanner.planNudge(savedDaysAgo?.let { at(daysAgo(it), 12) }, now, lines, settings, zone)

    // ---- due reminder

    @Test fun dueReminderCarriesTheCount() =
        assertEquals(NotificationPlan.DueReminder(12), NotificationPlanner.planDueReminder(12, NotificationSettings()))

    @Test fun noDueCardsSendsNothing() =
        assertEquals(NotificationPlan.None, NotificationPlanner.planDueReminder(0, NotificationSettings()))

    @Test fun disabledDueReminderSendsNothing() =
        assertEquals(
            NotificationPlan.None,
            NotificationPlanner.planDueReminder(5, NotificationSettings(dueRemindersEnabled = false)),
        )

    // ---- nudge

    @Test fun savedTwoDaysAgoIsTooRecent() = assertEquals(NotificationPlan.None, nudge(savedDaysAgo = 2))

    @Test fun savedThreeDaysAgoNudgesWithFirstLine() = assertEquals(NotificationPlan.Nudge(0), nudge(savedDaysAgo = 3))

    @Test fun nudgeOneDayAgoBlocksAnother() =
        assertEquals(
            NotificationPlan.None,
            nudge(10, NotificationSettings(lastNudgeEpochDay = daysAgo(1).toEpochDay(), firstSeenEpochDay = daysAgo(30).toEpochDay())),
        )

    @Test fun nudgeTwoDaysAgoAllowsAnother() =
        assertEquals(
            NotificationPlan.Nudge(4),
            nudge(
                10,
                NotificationSettings(
                    lastNudgeEpochDay = daysAgo(2).toEpochDay(),
                    lastNudgeLine = 3,
                    firstSeenEpochDay = daysAgo(30).toEpochDay(),
                ),
            ),
        )

    @Test fun linesWrapAndNeverRepeat() =
        assertEquals(
            NotificationPlan.Nudge(0),
            nudge(10, NotificationSettings(lastNudgeLine = 7, firstSeenEpochDay = daysAgo(30).toEpochDay())),
        )

    @Test fun singleLineStillWorks() =
        assertEquals(
            NotificationPlan.Nudge(0),
            nudge(10, NotificationSettings(lastNudgeLine = 0, firstSeenEpochDay = daysAgo(30).toEpochDay()), lines = 1),
        )

    @Test fun disabledNudgesSendNothing() =
        assertEquals(
            NotificationPlan.None,
            nudge(10, NotificationSettings(nudgesEnabled = false, firstSeenEpochDay = daysAgo(30).toEpochDay())),
        )

    @Test fun neverSavedCountsFromFirstSeen() {
        assertEquals(
            NotificationPlan.Nudge(0),
            nudge(null, NotificationSettings(firstSeenEpochDay = daysAgo(3).toEpochDay())),
        )
        assertEquals(
            NotificationPlan.None,
            nudge(null, NotificationSettings(firstSeenEpochDay = daysAgo(1).toEpochDay())),
        )
    }

    @Test fun oldSaveDoesNotNudgeRightAfterFirstSeen() =
        assertEquals(
            NotificationPlan.None,
            nudge(20, NotificationSettings(firstSeenEpochDay = daysAgo(1).toEpochDay())),
        )

    @Test fun noReferenceSendsNothing() = assertEquals(NotificationPlan.None, nudge(null, NotificationSettings()))

    // ---- next run

    private fun hoursUntil(hour: Int, minuteOfDay: Int) =
        NotificationPlanner.delayUntilNextMillis(at(today, hour), minuteOfDay, zone) / 3_600_000.0

    @Test fun nextRunLaterToday() = assertEquals(1.0, hoursUntil(8, 9 * 60), 0.0001)

    @Test fun nextRunAtExactTimeIsTomorrow() = assertEquals(24.0, hoursUntil(9, 9 * 60), 0.0001)

    @Test fun nextRunAfterTimeIsTomorrow() = assertEquals(23.0, hoursUntil(10, 9 * 60), 0.0001)
}