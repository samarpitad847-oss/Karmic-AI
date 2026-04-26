package com.karmicai.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * Stores computed burnout score only — raw PHQ answers are discarded after scoring.
 * This ensures no identifiable sensitive data is retained.
 */
@Entity
@Table(name = "burnout_scores")
@Data
@NoArgsConstructor
public class BurnoutScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Links to user anonymously (no foreign key name exposure)
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate scoreDate;

    // Score 0–20
    private int totalScore;

    @Enumerated(EnumType.STRING)
    private BurnoutLevel level;

    // PHQ sub-scores stored in aggregate (not raw responses)
    private int phqScore;       // 0–12 (q1–q5 × 0-3 scale mapped)
    private int sleepScore;     // 0–4
    private int stressScore;    // 0–4

    public enum BurnoutLevel {
        LOW, MODERATE, HIGH, CRITICAL;

        public static BurnoutLevel fromScore(int score) {
            if (score <= 5)  return LOW;
            if (score <= 10) return MODERATE;
            if (score <= 15) return HIGH;
            return CRITICAL;
        }
    }
}
