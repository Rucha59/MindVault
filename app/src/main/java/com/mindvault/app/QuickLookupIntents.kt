package com.mindvault.app

import android.content.Intent

object QuickLookupIntents {

    /** Private extra the trampoline uses to hand the cleaned text to MainActivity. */
    const val EXTRA_LOOKUP_TEXT = "com.mindvault.extra.QUICK_LOOKUP_TEXT"

    /**
     * The raw text of a "selected text" or "Share text" intent, or null for any other intent.
     * Callers must still clean it with QuickLookupParser.
     */
    fun incomingText(intent: Intent?): CharSequence? = when (intent?.action) {
        Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
        Intent.ACTION_SEND ->
            if (intent.type?.startsWith("text/") == true) intent.getCharSequenceExtra(Intent.EXTRA_TEXT) else null
        else -> null
    }
}