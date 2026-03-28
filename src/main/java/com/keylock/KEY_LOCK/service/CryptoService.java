package com.keylock.KEY_LOCK.service;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * CryptoService
 * Handles RSA-2048 key pair generation and AES-256 encryption/decryption.
 */
@Service
public class CryptoService {

    // -------------------------------------------------------
    // RSA Key Generation
    // -------------------------------------------------------

    /**
     * Generates a new RSA-2048 key pair and returns the PUBLIC key as PEM string.
     * The private key is NOT stored — the user downloads and keeps it.
     */
    public KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        return keyPairGen.generateKeyPair();
    }

    /**
     * Converts a PublicKey to PEM format string.
     */
    public String publicKeyToPem(PublicKey publicKey) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(publicKey.getEncoded());
        return "-----BEGIN RSA PUBLIC KEY-----\n" + base64 + "\n-----END RSA PUBLIC KEY-----";
    }

    /**
     * Converts a PrivateKey to PEM format string.
     */
    public String privateKeyToPem(PrivateKey privateKey) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(privateKey.getEncoded());
        return "-----BEGIN RSA PRIVATE KEY-----\n" + base64 + "\n-----END RSA PRIVATE KEY-----";
    }

    /**
     * Parses a PEM public key string back into a PublicKey object.
     */
    public PublicKey pemToPublicKey(String pem) throws Exception {
        String cleaned = pem
                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(cleaned);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
    }

    /**
     * Parses a PEM private key string back into a PrivateKey object.
     */
    public PrivateKey pemToPrivateKey(String pem) throws Exception {
        String cleaned = pem
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(cleaned);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePrivate(spec);
    }

    // -------------------------------------------------------
    // AES-256 File Encryption
    // -------------------------------------------------------

    /**
     * Generates a random AES-256 session key.
     */
    public SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        return keyGen.generateKey();
    }

    /**
     * Encrypts file bytes using AES-256.
     */
    public byte[] encryptAES(byte[] data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    /**
     * Decrypts AES-256 encrypted bytes.
     */
    public byte[] decryptAES(byte[] encryptedData, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(encryptedData);
    }

    /**
     * Encrypts an AES key using RSA public key (for secure key distribution).
     */
    public byte[] encryptAESKeyWithRSA(SecretKey aesKey, PublicKey rsaPublicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
        return cipher.doFinal(aesKey.getEncoded());
    }

    /**
     * Decrypts an AES key using RSA private key.
     */
    public SecretKey decryptAESKeyWithRSA(byte[] encryptedAESKey, PrivateKey rsaPrivateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
        byte[] aesKeyBytes = cipher.doFinal(encryptedAESKey);
        return new SecretKeySpec(aesKeyBytes, "AES");
    }

    // -------------------------------------------------------
    // SHA-256 Integrity Hashing
    // -------------------------------------------------------

    /**
     * Computes SHA-256 hash of file bytes for integrity verification.
     */
    public String computeSHA256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    /**
     * Verifies file integrity by comparing SHA-256 hashes.
     */
    public boolean verifyIntegrity(byte[] data, String expectedHash) throws NoSuchAlgorithmException {
        String actualHash = computeSHA256(data);
        return actualHash.equals(expectedHash);
    }

    // -------------------------------------------------------
    // Utility: Validate RSA Key format
    // -------------------------------------------------------

    /**
     * Checks if a given string looks like a valid RSA key (basic validation).
     */
    public boolean isValidRSAKeyFormat(String key) {
        if (key == null || key.trim().isEmpty()) return false;
        String trimmed = key.trim();
        return trimmed.contains("-----BEGIN RSA") && trimmed.contains("-----END RSA");
    }
}
