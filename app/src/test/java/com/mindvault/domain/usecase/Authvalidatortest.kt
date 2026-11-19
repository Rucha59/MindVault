package com.mindvault.domain.usecase

import com.mindvault.domain.model.AuthFieldError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidatorTest {
    @Test fun emailIsTrimmedAndLowercased() =
        assertEquals("a@b.com", AuthValidator.normalizeEmail("  A@B.com "))

    @Test fun emptyEmailRejected() = assertEquals(AuthFieldError.EmailEmpty, AuthValidator.validateEmail("   "))

    @Test fun malformedEmailsRejected() {
        listOf("abc", "a@b", "a b@c.com", "@c.com", "a@@c.com").forEach {
            assertEquals(it, AuthFieldError.EmailInvalid, AuthValidator.validateEmail(it))
        }
    }

    @Test fun overlongEmailRejected() =
        assertEquals(AuthFieldError.EmailTooLong, AuthValidator.validateEmail("a".repeat(250) + "@b.com"))

    @Test fun validEmailAccepted() = assertNull(AuthValidator.validateEmail("rucha@example.com"))

    @Test fun signUpPasswordMinimumIsEight() {
        assertEquals(AuthFieldError.PasswordTooShort, AuthValidator.validatePassword("1234567", isSignUp = true))
        assertNull(AuthValidator.validatePassword("12345678", isSignUp = true))
    }

    @Test fun signUpPasswordMaximumIs72Bytes() {
        assertNull(AuthValidator.validatePassword("a".repeat(72), isSignUp = true))
        assertEquals(AuthFieldError.PasswordTooLong, AuthValidator.validatePassword("a".repeat(73), isSignUp = true))
    }

    @Test fun signInOnlyRequiresAPassword() {
        assertEquals(AuthFieldError.PasswordEmpty, AuthValidator.validatePassword("", isSignUp = false))
        assertNull(AuthValidator.validatePassword("short", isSignUp = false))
    }

    @Test fun passwordIsNeverTrimmed() = assertNull(AuthValidator.validatePassword("       a", isSignUp = true))
}