package com.mindvault.domain.usecase

import com.mindvault.domain.model.SyncProblem
import com.mindvault.domain.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncStatusBuilderTest {
    @Test
    fun syncingWinsOverEverythingElse() {
        assertEquals(SyncStatus.Syncing, SyncStatusBuilder.build(true, 3, 10L, SyncProblem.OFFLINE))
    }

    @Test
    fun aProblemIsShownEvenWhenNothingIsWaiting() {
        assertEquals(
            SyncStatus.NeedsAttention(SyncProblem.OFFLINE, 0, 10L),
            SyncStatusBuilder.build(false, 0, 10L, SyncProblem.OFFLINE),
        )
    }

    @Test
    fun waitingChangesAreNeverReportedAsSynced() {
        assertEquals(SyncStatus.Waiting(2), SyncStatusBuilder.build(false, 2, 10L, null))
    }

    @Test
    fun syncedOnlyAfterASuccessWithNothingWaiting() {
        assertEquals(SyncStatus.Synced(10L), SyncStatusBuilder.build(false, 0, 10L, null))
        assertEquals(SyncStatus.NotSyncedYet, SyncStatusBuilder.build(false, 0, null, null))
    }
}