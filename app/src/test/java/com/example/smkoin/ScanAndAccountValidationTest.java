package com.example.smkoin;

import org.junit.Test;
import static org.junit.Assert.*;

public class ScanAndAccountValidationTest {
    @Test public void qrPayloadAllowsOpaqueSchemaSupportedText() {
        assertTrue(SupabaseRepository.isValidQrPayload("merchant:receipt:abc-123"));
        assertTrue(SupabaseRepository.isValidQrPayload("https://example.test/receipt?id=42"));
    }

    @Test public void qrPayloadRejectsEmptyOversizedAndControlText() {
        assertFalse(SupabaseRepository.isValidQrPayload(null));
        assertFalse(SupabaseRepository.isValidQrPayload("  \n"));
        assertFalse(SupabaseRepository.isValidQrPayload("abc\u0000def"));
        assertFalse(SupabaseRepository.isValidQrPayload(new String(new char[2049]).replace('\0', 'a')));
    }

    @Test public void profileNameValidationUsesSchemaSafeBounds() {
        assertEquals("Enter your name.", AccountInputValidator.fullNameError("  "));
        assertNull(AccountInputValidator.fullNameError("Ari Putra"));
        assertNotNull(AccountInputValidator.fullNameError(new String(new char[101]).replace('\0', 'a')));
    }

    @Test public void passwordRequiresLengthDifferenceAndMatchingConfirmation() {
        assertNotNull(AccountInputValidator.newPasswordError("old12345", "short"));
        assertNotNull(AccountInputValidator.newPasswordError("same-pass", "same-pass"));
        assertNull(AccountInputValidator.newPasswordError("old12345", "new-pass"));
        assertNotNull(AccountInputValidator.passwordConfirmationError("new-pass", "mismatch"));
        assertNull(AccountInputValidator.passwordConfirmationError("new-pass", "new-pass"));
    }
}
