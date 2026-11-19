package com.mindvault.data.repository

import com.mindvault.core.util.nowMillis

/**
 * The updatedAt for a new edit. Never earlier than the previous version plus 1 ms, so an edit made after the
 * device clock moved backwards still counts as newer than the version it replaces when sync resolves conflicts.
 */
internal fun nextUpdatedAt(previous: Long): Long = maxOf(nowMillis(), previous + 1)