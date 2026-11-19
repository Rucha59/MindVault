package com.mindvault.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Small key/value store for sync bookkeeping (per-table pull cursors, whether the first full upload is done,
 * last success time, last error). Created in schema v2 so the sync engine (Phase C2) needs no second
 * migration. Nothing reads or writes it yet.
 */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val stateKey: String,
    val stateValue: String,
)