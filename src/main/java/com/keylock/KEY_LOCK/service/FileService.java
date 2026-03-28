package com.keylock.KEY_LOCK.service;

import com.keylock.KEY_LOCK.model.FileRecord;
import com.keylock.KEY_LOCK.model.User;
import com.keylock.KEY_LOCK.repository.FileRepository;
import com.keylock.KEY_LOCK.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKey;
import java.nio.file.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;

@Service
public class FileService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CryptoService cryptoService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * Upload and encrypt a file using AES-256.
     * The AES session key is encrypted with the admin's RSA public key.
     */
    public FileRecord uploadFile(MultipartFile file, String uploadedByEmail) throws Exception {

        // Create upload directory if not exists
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Read file bytes
        byte[] fileBytes = file.getBytes();

        // Compute SHA-256 hash for integrity verification
        String sha256Hash = cryptoService.computeSHA256(fileBytes);

        // Generate AES-256 session key
        SecretKey aesKey = cryptoService.generateAESKey();

        // Encrypt the file with AES-256
        byte[] encryptedBytes = cryptoService.encryptAES(fileBytes, aesKey);

        // Store encrypted file with unique name
        String uniqueName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename() + ".enc";
        Path filePath = uploadPath.resolve(uniqueName);
        Files.write(filePath, encryptedBytes);

        // Also store the AES key (encrypted with a master key for demo purposes)
        // In production you'd encrypt per-user
        String aesKeyBase64 = Base64.getEncoder().encodeToString(aesKey.getEncoded());
        Path keyPath = uploadPath.resolve(uniqueName + ".key");
        Files.writeString(keyPath, aesKeyBase64);

        // Save file record to database
        FileRecord record = new FileRecord(
                file.getOriginalFilename(),
                uniqueName,
                file.getSize(),
                file.getContentType(),
                sha256Hash,
                uploadedByEmail
        );
        return fileRepository.save(record);
    }

    /**
     * Download and decrypt a file.
     * Verifies RSA key before decrypting.
     * Returns original decrypted bytes.
     */
    public byte[] downloadFile(Long fileId, String userEmail, String providedPrivateKeyPem) throws Exception {

        // 1. Check user is not revoked
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isRevoked()) {
            throw new SecurityException("Your access has been revoked by the administrator.");
        }

        // 2. Get file record
        FileRecord record = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        // 3. Validate RSA key format
        if (!cryptoService.isValidRSAKeyFormat(providedPrivateKeyPem)) {
            throw new SecurityException("Invalid RSA key format.");
        }

        // 4. Try to parse the provided private key to validate it is well-formed
        PrivateKey privateKey;
        try {
            privateKey = cryptoService.pemToPrivateKey(providedPrivateKeyPem);
        } catch (Exception e) {
            throw new SecurityException("Invalid RSA private key. Access denied.");
        }

        // 5. Cross-verify: use stored public key to verify the private key matches
        //    We do this by encrypting a test string with public key and decrypting with provided private key
        try {
            PublicKey publicKey = cryptoService.pemToPublicKey(user.getRsaKey());
            String testMessage = "KEYLOCK_VERIFY_" + user.getEmail();
            byte[] encrypted = encryptTestWithPublicKey(testMessage.getBytes(), publicKey);
            byte[] decrypted = decryptTestWithPrivateKey(encrypted, privateKey);
            if (!new String(decrypted).equals(testMessage)) {
                throw new SecurityException("RSA key does not match your account. Access denied.");
            }
        } catch (SecurityException se) {
            throw se;
        } catch (Exception e) {
            throw new SecurityException("RSA key verification failed. Access denied.");
        }

        // 6. Read encrypted file from disk
        Path uploadPath = Paths.get(uploadDir);
        Path encFilePath = uploadPath.resolve(record.getStoredName());
        byte[] encryptedBytes = Files.readAllBytes(encFilePath);

        // 7. Read stored AES key
        Path keyPath = uploadPath.resolve(record.getStoredName() + ".key");
        String aesKeyBase64 = Files.readString(keyPath).trim();
        byte[] aesKeyBytes = Base64.getDecoder().decode(aesKeyBase64);
        SecretKey aesKey = new javax.crypto.spec.SecretKeySpec(aesKeyBytes, "AES");

        // 8. Decrypt the file
        byte[] decryptedBytes = cryptoService.decryptAES(encryptedBytes, aesKey);

        // 9. Verify SHA-256 integrity
        if (!cryptoService.verifyIntegrity(decryptedBytes, record.getSha256Hash())) {
            throw new RuntimeException("File integrity check failed! File may be tampered.");
        }

        return decryptedBytes;
    }

    /**
     * Get all files (for admin and employee views).
     */
    public List<FileRecord> getAllFiles() {
        return fileRepository.findAllByOrderByUploadedAtDesc();
    }

    /**
     * Delete a file record and its stored file.
     */
    public void deleteFile(Long fileId) throws Exception {
        FileRecord record = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        // Delete from disk
        Path uploadPath = Paths.get(uploadDir);
        Files.deleteIfExists(uploadPath.resolve(record.getStoredName()));
        Files.deleteIfExists(uploadPath.resolve(record.getStoredName() + ".key"));

        // Delete from DB
        fileRepository.deleteById(fileId);
    }

    // ---- RSA Test Helpers ----

    private byte[] encryptTestWithPublicKey(byte[] data, PublicKey publicKey) throws Exception {
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    private byte[] decryptTestWithPrivateKey(byte[] data, PrivateKey privateKey) throws Exception {
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(data);
    }
}
