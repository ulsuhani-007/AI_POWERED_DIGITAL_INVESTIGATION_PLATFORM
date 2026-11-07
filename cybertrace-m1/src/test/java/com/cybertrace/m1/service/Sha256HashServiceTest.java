package com.cybertrace.m1.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class Sha256HashServiceTest {

    private Sha256HashService sha256HashService;

    @BeforeEach
    void setUp() {
        sha256HashService = new Sha256HashService();
    }

    @Test
    void testCalculateHashKnownString() {
        // Known SHA-256 for "hello world"
        // b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9
        String input = "hello world";
        String hash = sha256HashService.calculateHash(input.getBytes(StandardCharsets.UTF_8));
        assertEquals("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9", hash);
    }

    @Test
    void testCalculateHashInputStream() throws IOException {
        String input = "forensic evidence test";
        ByteArrayInputStream stream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        String hash = sha256HashService.calculateHash(stream);

        assertNotNull(hash);
        assertEquals(64, hash.length());
        assertEquals(sha256HashService.calculateHash(input.getBytes(StandardCharsets.UTF_8)), hash);
    }

    @Test
    void testCalculateHashNullBytesThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> sha256HashService.calculateHash((byte[]) null));
    }

    @Test
    void testCalculateHashNullStreamThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> sha256HashService.calculateHash((ByteArrayInputStream) null));
    }
}

