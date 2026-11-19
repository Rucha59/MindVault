package com.mindvault.domain.model

/** Whose on-device vault is open: the guest vault or one account's vault. */
sealed interface DataOwner {
    data object Guest : DataOwner
    data class Account(val userId: String) : DataOwner
}