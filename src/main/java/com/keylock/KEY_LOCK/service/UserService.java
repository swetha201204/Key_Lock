package com.keylock.KEY_LOCK.service;

import com.keylock.KEY_LOCK.model.User;
import com.keylock.KEY_LOCK.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CryptoService cryptoService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Registers a new user, generates RSA key pair.
     * Returns the PRIVATE key PEM so the user can download it once.
     * Only the PUBLIC key is stored in the database.
     */
    public String registerUser(String name, String email, String rawPassword, String role) throws Exception {

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered: " + email);
        }

        // Generate RSA-2048 key pair
        KeyPair keyPair = cryptoService.generateRSAKeyPair();
        String publicKeyPem  = cryptoService.publicKeyToPem(keyPair.getPublic());
        String privateKeyPem = cryptoService.privateKeyToPem(keyPair.getPrivate());

        // Encode password
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Determine role
        User.Role userRole = role.equalsIgnoreCase("ADMIN") ? User.Role.ADMIN : User.Role.EMPLOYEE;

        // Save user with PUBLIC key only
        User user = new User(name, email, encodedPassword, userRole, publicKeyPem);
        userRepository.save(user);

        // Return PRIVATE key to show once to the user
        return privateKeyPem;
    }

    /**
     * Find user by email.
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Get all employees.
     */
    public List<User> getAllEmployees() {
        return userRepository.findByRole(User.Role.EMPLOYEE);
    }

    /**
     * Get all users.
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Revoke a user's access.
     */
    public void revokeUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRevoked(true);
        userRepository.save(user);
    }

    /**
     * Restore a revoked user.
     */
    public void restoreUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRevoked(false);
        userRepository.save(user);
    }

    /**
     * Check if a user is revoked.
     */
    public boolean isRevoked(String email) {
        return userRepository.findByEmail(email)
                .map(User::isRevoked)
                .orElse(false);
    }
}
