package com.karmicai.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Hashed email — never stored in plaintext
    @Column(nullable = false, unique = true)
    private String emailHash;

    @Column(nullable = false)
    private String passwordHash;

    // Anonymous ID shown in peer rooms
    @Column(nullable = false, unique = true)
    private String anonymousId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String institution;
    private String department;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    // Feature flags
    private boolean womenSafeMode = false;
    private boolean anonymousInPeer = true;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime lastCheckinAt;

    public enum Role {
        INTERN, JUNIOR_RESIDENT, SENIOR_RESIDENT, CONSULTANT, ADMIN
    }

    public enum Gender {
        FEMALE, MALE, NON_BINARY, PREFER_NOT
    }
}
