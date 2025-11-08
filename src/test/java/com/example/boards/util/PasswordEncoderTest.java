package com.example.boards.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PasswordEncoderTest {

    @Test
    public void testEncode() {
        // Given
        String password = "test1234";

        // When
        String encoded = PasswordEncoder.encode(password);

        // Then
        assertNotNull(encoded);
        assertEquals(64, encoded.length()); // SHA-256 produces 64 character hex string
        assertNotEquals(password, encoded);
    }

    @Test
    public void testEncodeSamePasswordProducesSameHash() {
        // Given
        String password = "test1234";

        // When
        String encoded1 = PasswordEncoder.encode(password);
        String encoded2 = PasswordEncoder.encode(password);

        // Then
        assertEquals(encoded1, encoded2);
    }

    @Test
    public void testEncodeDifferentPasswordsProduceDifferentHashes() {
        // Given
        String password1 = "test1234";
        String password2 = "test5678";

        // When
        String encoded1 = PasswordEncoder.encode(password1);
        String encoded2 = PasswordEncoder.encode(password2);

        // Then
        assertNotEquals(encoded1, encoded2);
    }

    @Test
    public void testMatches() {
        // Given
        String password = "test1234";
        String encoded = PasswordEncoder.encode(password);

        // When & Then
        assertTrue(PasswordEncoder.matches(password, encoded));
    }

    @Test
    public void testMatchesWrongPassword() {
        // Given
        String password = "test1234";
        String wrongPassword = "wrong1234";
        String encoded = PasswordEncoder.encode(password);

        // When & Then
        assertFalse(PasswordEncoder.matches(wrongPassword, encoded));
    }

    @Test
    public void testEncodeEmptyPassword() {
        // Given
        String password = "";

        // When
        String encoded = PasswordEncoder.encode(password);

        // Then
        assertNotNull(encoded);
        assertEquals(64, encoded.length());
    }
}

