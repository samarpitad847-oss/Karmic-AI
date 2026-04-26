package com.karmicai.service;

import com.karmicai.model.User;
import com.karmicai.model.User.Gender;
import com.karmicai.model.User.Role;
import com.karmicai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public User register(String email, String password, Role role, String institution,
                         String department, Gender gender) throws Exception {

        String emailHash = sha256(email.toLowerCase().trim());
        if (userRepo.existsByEmailHash(emailHash)) {
            throw new IllegalArgumentException("Account already exists.");
        }

        User user = new User();
        user.setEmailHash(emailHash);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setAnonymousId("anon-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        user.setRole(role);
        user.setInstitution(institution);
        user.setDepartment(department);
        user.setGender(gender);
        // Auto-enable Women-Safe mode for female-identifying users
        if (gender == Gender.FEMALE) user.setWomenSafeMode(true);

        return userRepo.save(user);
    }

    public User authenticate(String email, String password) throws Exception {
        String emailHash = sha256(email.toLowerCase().trim());
        User user = userRepo.findByEmailHash(emailHash)
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials."));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials.");
        }
        return user;
    }

    public void toggleWomenMode(Long userId, boolean enabled) {
        userRepo.findById(userId).ifPresent(u -> {
            u.setWomenSafeMode(enabled);
            userRepo.save(u);
        });
    }

    public void toggleAnonPeer(Long userId, boolean anonymous) {
        userRepo.findById(userId).ifPresent(u -> {
            u.setAnonymousInPeer(anonymous);
            userRepo.save(u);
        });
    }

    /**
     * Returns role-specific dashboard configuration.
     * Routes different tool sets and intervention thresholds based on role.
     */
    public Map<String, Object> getRoleConfig(User user) {
        return switch (user.getRole()) {
            case INTERN -> Map.of(
                "microTools", new String[]{"Box Breathing (4 min)", "Reset Audio", "3-Line Journal"},
                "checkInFrequency", "DAILY",
                "escalationThreshold", 12,
                "peerRoomsHighlighted", new String[]{"firstyear", "exams", "general"}
            );
            case JUNIOR_RESIDENT -> Map.of(
                "microTools", new String[]{"Box Breathing", "Sleep Hygiene Kit", "Reflective Journal"},
                "checkInFrequency", "DAILY",
                "escalationThreshold", 13,
                "peerRoomsHighlighted", new String[]{"nightshift", "icu", "exams"}
            );
            case SENIOR_RESIDENT -> Map.of(
                "microTools", new String[]{"Stress Reset (2 min)", "Journal", "Boundary-Setting Guide"},
                "checkInFrequency", "DAILY",
                "escalationThreshold", 14,
                "peerRoomsHighlighted", new String[]{"surgery", "icu", "general"}
            );
            case CONSULTANT -> Map.of(
                "microTools", new String[]{"Breathing Reset", "Leadership Journal", "iCall Helpline"},
                "checkInFrequency", "TWICE_WEEKLY",
                "escalationThreshold", 15,
                "peerRoomsHighlighted", new String[]{"general", "harassment"}
            );
            case ADMIN -> Map.of(
                "redirectTo", "admin.html",
                "checkInFrequency", "NONE"
            );
        };
    }

    private String sha256(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes());
        return HexFormat.of().formatHex(hash);
    }
}
