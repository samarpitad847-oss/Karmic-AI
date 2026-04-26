package com.karmicai.service;

import com.karmicai.model.BurnoutScore;
import com.karmicai.model.BurnoutScore.BurnoutLevel;
import com.karmicai.repository.BurnoutScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BurnoutService {

    private final BurnoutScoreRepository burnoutRepo;

    /**
     * Calculate burnout score from raw check-in answers.
     * Raw answers are NEVER persisted — only the computed score.
     */
    public BurnoutScore calculateAndSave(Long userId, Map<String, Object> answers) {
        // PHQ-9 style items: q1–q5, each 0–3
        int phq = 0;
        for (String key : new String[]{"q1", "q2", "q3", "q4", "q5"}) {
            phq += toInt(answers.get(key));
        }

        // Sleep: q6 = hours slept (0–16)
        int hoursSlept = toInt(answers.get("q6"));
        int sleepScore;
        if (hoursSlept < 4)      sleepScore = 4;
        else if (hoursSlept < 6) sleepScore = 3;
        else if (hoursSlept < 7) sleepScore = 2;
        else if (hoursSlept < 9) sleepScore = 1;
        else                     sleepScore = 0;

        // Stress: q7 = 0–10 → 0–4
        int stressRaw = toInt(answers.get("q7"));
        int stressScore = Math.min(4, stressRaw / 3);

        int total = phq + sleepScore + stressScore;
        total = Math.min(20, total); // cap

        BurnoutScore score = new BurnoutScore();
        score.setUserId(userId);
        score.setScoreDate(LocalDate.now());
        score.setTotalScore(total);
        score.setPhqScore(phq);
        score.setSleepScore(sleepScore);
        score.setStressScore(stressScore);
        score.setLevel(BurnoutLevel.fromScore(total));

        return burnoutRepo.save(score);
    }

    /** Returns latest score for user. */
    public BurnoutScore getLatest(Long userId) {
        return burnoutRepo.findTopByUserIdOrderByScoreDateDesc(userId);
    }

    /** 7-day trend: positive = worsening, negative = improving. */
    public int computeTrend(Long userId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(14);
        List<BurnoutScore> recent = burnoutRepo.findByUserIdAndScoreDateBetween(userId, start, end);
        if (recent.size() < 2) return 0;
        // Compare avg of last 3 vs prior 3
        double recent3 = recent.stream().skip(Math.max(0, recent.size() - 3))
                               .mapToInt(BurnoutScore::getTotalScore).average().orElse(0);
        double prior3  = recent.stream().limit(Math.min(3, recent.size() - 3))
                               .mapToInt(BurnoutScore::getTotalScore).average().orElse(0);
        return (int) Math.round(recent3 - prior3);
    }

    /**
     * Predict next-7-day burnout direction using a simple weighted-average extrapolation.
     * This is not ML — it's a rule-based heuristic suitable for an MVP.
     */
    public String predictTrend(Long userId) {
        int trend = computeTrend(userId);
        BurnoutScore latest = getLatest(userId);
        if (latest == null) return "STABLE";

        int score = latest.getTotalScore();
        if (score >= 15 && trend >= 0) return "CRITICAL_RISK";
        if (trend >= 2)                return "WORSENING";
        if (trend <= -2)               return "IMPROVING";
        return "STABLE";
    }

    /** Aggregate dept-level stats for admin panel — no individual data returned. */
    public Map<String, Object> getDeptAggregate(String institution, String department) {
        LocalDate since = LocalDate.now().minusDays(7);
        List<BurnoutScore> scores = burnoutRepo.findByInstitutionAndDeptSince(institution, department, since);
        if (scores.isEmpty()) return Map.of("level", "UNKNOWN", "avg", 0, "count", 0);

        double avg = scores.stream().mapToInt(BurnoutScore::getTotalScore).average().orElse(0);
        BurnoutLevel level = BurnoutLevel.fromScore((int) Math.round(avg));
        return Map.of("level", level.name(), "avg", Math.round(avg * 10.0) / 10.0, "count", scores.size());
    }

    /** Check if a user needs emergency escalation (3+ consecutive HIGH/CRITICAL days). */
    public boolean needsEscalation(Long userId) {
        List<BurnoutScore> last3 = burnoutRepo.findTop3ByUserIdOrderByScoreDateDesc(userId);
        if (last3.size() < 3) return false;
        return last3.stream().allMatch(s -> s.getLevel() == BurnoutLevel.HIGH || s.getLevel() == BurnoutLevel.CRITICAL);
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (NumberFormatException e) { return 0; }
    }
}
