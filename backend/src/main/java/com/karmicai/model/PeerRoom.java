package com.karmicai.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a peer support room.
 * Messages are ephemeral (24h TTL) — this entity tracks room metadata only.
 * No individual user data is stored in rooms; only anonymousId references.
 */
@Entity
@Table(name = "peer_rooms")
@Data
@NoArgsConstructor
public class PeerRoom {

    @Id
    @Column(nullable = false, unique = true)
    private String id;  // e.g. "icu", "nightshift", "women"

    @Column(nullable = false)
    private String name;

    private String description;

    private String icon;

    @Column(nullable = false)
    private String category;  // emergency, specialty, women, all

    /** If true, only visible to users with womenSafeMode enabled */
    private boolean womenOnly = false;

    /** Approximate online count — refreshed periodically, not exact */
    private int onlineCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime lastActivityAt;

    /**
     * Pre-seeded rooms — called by data initializer on startup.
     */
    public static List<PeerRoom> defaultRooms() {
        List<PeerRoom> rooms = new ArrayList<>();

        rooms.add(room("icu",        "ICU / Critical Care",      "🫀", "High-pressure care, moral distress",          "specialty", false, 12));
        rooms.add(room("nightshift", "Night Shift Survivors",     "🌙", "Fatigue, isolation, post-call blues",         "emergency", false, 8));
        rooms.add(room("emergency",  "Emergency Medicine",        "🚑", "Trauma, high-volume, burnout",                "emergency", false, 15));
        rooms.add(room("exams",      "PG Exam Pressure",          "📚", "NEET-PG, DNB, Diplomat stress",               "specialty", false, 22));
        rooms.add(room("women",      "Women Doctors Only",        "👩‍⚕️", "Safe space — gender-specific challenges",    "women",     true,  6));
        rooms.add(room("harassment", "Workplace Concerns",        "🛡️", "Anonymous space for reporting & support",    "emergency", false, 4));
        rooms.add(room("firstyear",  "First Year Interns",        "🎓", "New to clinical life, finding your feet",    "specialty", false, 18));
        rooms.add(room("surgery",    "Surgical Residents",        "🔬", "OR stress, hierarchy, long hours",           "specialty", false, 9));
        rooms.add(room("general",    "General Wellbeing",         "💚", "Open chat — anything goes",                  "all",       false, 31));

        return rooms;
    }

    private static PeerRoom room(String id, String name, String icon,
                                  String desc, String category,
                                  boolean womenOnly, int online) {
        PeerRoom r = new PeerRoom();
        r.setId(id);
        r.setName(name);
        r.setIcon(icon);
        r.setDescription(desc);
        r.setCategory(category);
        r.setWomenOnly(womenOnly);
        r.setOnlineCount(online);
        r.setLastActivityAt(LocalDateTime.now());
        return r;
    }
}
