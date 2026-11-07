package com.cybertrace.m1.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class Sha256HashService {

    private static final String ALGORITHM = "SHA-256";

    /**
     * Calculates the SHA-256 cryptographic hash of the provided byte array.
     *
     * @param data bytes to hash
     * @return 64-character lowercase hex string
     */
    public String calculateHash(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data to hash cannot be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(data);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in current JVM", e);
        }
    }

    /**
     * Calculates the SHA-256 cryptographic hash from an InputStream.
     *
     * @param inputStream stream to read from
     * @return 64-character lowercase hex string
     * @throws IOException on I/O error
     */
    public String calculateHash(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in current JVM", e);
        }
    }
}

