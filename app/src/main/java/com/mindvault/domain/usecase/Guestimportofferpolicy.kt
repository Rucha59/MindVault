package com.mindvault.domain.usecase

/** When to offer the guest-to-account import. Pure, so it is unit tested. */
object GuestImportOfferPolicy {
    /**
     * The number of guest items to offer, or null for no offer. The offer waits for the account's first sync, so
     * items already in the cloud are known and can be skipped, and it is shown once per account on this device.
     */
    fun itemsToOffer(
        isAccountVault: Boolean,
        hasSynced: Boolean,
        alreadyHandled: Boolean,
        guestItemCount: Int,
    ): Int? = guestItemCount.takeIf { isAccountVault && hasSynced && !alreadyHandled && it > 0 }
}