package com.keylock.KEY_LOCK.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "rsa_key", columnDefinition = "TEXT")
    private String rsaKey;

    @Column(name = "is_revoked")
    private boolean revoked = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ---- Enum ----
    public enum Role {
        ADMIN, EMPLOYEE
    }

    // ---- Constructors ----
    public User() {}

    public User(String name, String email, String password, Role role, String rsaKey) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.rsaKey = rsaKey;
        this.createdAt = LocalDateTime.now();
    }

    // ---- Getters & Setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getRsaKey() { return rsaKey; }
    public void setRsaKey(String rsaKey) { this.rsaKey = rsaKey; }

    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
