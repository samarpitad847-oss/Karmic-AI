package com.karmicai.controller;

import com.karmicai.model.BurnoutScore;
import com.karmicai.model.User;
import com.karmicai.repository.UserRepository;
import com.karmicai.service.BurnoutService;
import com.karmicai.service.JwtService;
import com.karmicai.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final BurnoutService burnoutService;
    private final UserService    userService;
    private final JwtService     jwtService;
    private final UserRepository userRepository;

    @PostMapping("/checkin")
    public ResponseEntity<?> checkIn(
        @RequestHeader("Authorization") String auth,
        @RequestBody Map<String, Object> answers
    ) {
        Long userId = jwtService.extractUserId(auth);
        BurnoutScore score = burnoutService.calculateAndSave(userId, answers);
        boolean escalate = burnoutService.needsEscalation(userId);

        return ResponseEntity.ok(Map.of(
            "burnoutLevel",    score.getLevel().name(),
            "score",           score.getTotalScore(),
            "trend",           trendLabel(burnoutService.computeTrend(userId)),
            "trendValue",      burnoutService.computeTrend(userId),
            "prediction",      burnoutService.predictTrend(userId),
            "needsEscalation", escalate
        ));
    }

    @GetMapping("/burnout/latest")
    public ResponseEntity<?> getLatest(@RequestHeader("Authorization") String auth) {
        Long userId = jwtService.extractUserId(auth);
        BurnoutScore score = burnoutService.getLatest(userId);
        if (score == null) {
            return ResponseEntity.ok(Map.of("burnoutLevel", "UNKNOWN", "score", 0));
        }
        return ResponseEntity.ok(Map.of(
            "burnoutLevel", score.getLevel().name(),
            "score",        score.getTotalScore(),
            "trend",        trendLabel(burnoutService.computeTrend(userId)),
            "trendValue",   burnoutService.computeTrend(userId),
            "prediction",   burnoutService.predictTrend(userId)
        ));
    }

    @GetMapping("/burnout/role-config")
    public ResponseEntity<?> getRoleConfig(@RequestHeader("Authorization") String auth) {
        Long userId = jwtService.extractUserId(auth);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new SecurityException("User not found."));
        return ResponseEntity.ok(userService.getRoleConfig(user));
    }

    @PostMapping("/user/women-mode")
    public ResponseEntity<?> setWomenMode(
        @RequestHeader("Authorization") String auth,
        @RequestBody Map<String, Boolean> body
    ) {
        Long userId = jwtService.extractUserId(auth);
        userService.toggleWomenMode(userId, body.getOrDefault("enabled", false));
        return ResponseEntity.ok(Map.of("status", "updated"));
    }

    @PostMapping("/peer/anonymous")
    public ResponseEntity<?> setAnonymous(
        @RequestHeader("Authorization") String auth,
        @RequestBody Map<String, Boolean> body
    ) {
        Long userId = jwtService.extractUserId(auth);
        userService.toggleAnonPeer(userId, body.getOrDefault("anonymous", true));
        return ResponseEntity.ok(Map.of("status", "updated"));
    }

    private String trendLabel(int trend) {
        if (trend >= 2)  return "WORSENING";
        if (trend <= -2) return "IMPROVING";
        return "STABLE";
    }
}
