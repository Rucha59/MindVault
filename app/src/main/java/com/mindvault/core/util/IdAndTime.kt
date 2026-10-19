package com.mindvault.core.util

import java.util.UUID

/** Globally unique, safe to generate offline and to sync later. */
fun newId(): String = UUID.randomUUID().toString()

/** All timestamps are UTC epoch milliseconds. */
fun nowMillis(): Long = System.currentTimeMillis()