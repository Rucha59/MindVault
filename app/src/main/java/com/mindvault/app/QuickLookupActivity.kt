package com.mindvault.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.mindvault.R
import com.mindvault.domain.model.QuickLookupParser

/**
 * Entry point for "selected text" (MindVault in the selection menu) and "Share text".
 *
 * It has no screen. Android would open MainActivity inside the other app's task if it received these intents
 * directly, so this activity cleans the text and forwards it to MainActivity in MindVault's own task, then closes.
 */
class QuickLookupActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = QuickLookupParser.parse(QuickLookupIntents.incomingText(intent))
        if (text == null) {
            Toast.makeText(this, R.string.quick_lookup_nothing_selected, Toast.LENGTH_SHORT).show()
        } else {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(QuickLookupIntents.EXTRA_LOOKUP_TEXT, text),
            )
        }
        setResult(RESULT_CANCELED) // the selected text in the other app is never changed
        finish()
    }
}