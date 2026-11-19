package com.mindvault.data.local.database

import android.content.Context
import com.mindvault.domain.model.DataOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers which vault was open last (a plain user ID, or nothing for the guest vault) so the right
 * vault opens at app start. Excluded from backup (see res/xml/backup_rules.xml and data_extraction_rules.xml).
 */
@Singleton
class ActiveOwnerStore(context: Context, prefsName: String) {

    @Inject
    constructor(@ApplicationContext context: Context) : this(context, PREFS_NAME)

    private val prefs = context.applicationContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun read(): DataOwner =
        prefs.getString(KEY_OWNER, null)
            ?.takeIf { it.isNotBlank() }
            ?.let { DataOwner.Account(it) }
            ?: DataOwner.Guest

    fun write(owner: DataOwner) {
        val editor = prefs.edit()
        when (owner) {
            DataOwner.Guest -> editor.remove(KEY_OWNER)
            is DataOwner.Account -> editor.putString(KEY_OWNER, owner.userId)
        }
        editor.commit()
    }

    companion object {
        const val PREFS_NAME = "mindvault_active_owner"
        private const val KEY_OWNER = "owner"
    }
}