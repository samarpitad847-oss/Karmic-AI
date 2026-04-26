package com.karmicai.controller;

import com.karmicai.service.BurnoutService;
import com.karmicai.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AdminController — aggregate-only analytics for hospital administrators.
 *
 * PRIVACY GUARANTEE: No endpoint exposes individual user data.
 * All responses contain department or role-level aggregates only.
 * Minimum 5 responses required before any data is shown (enforced in BurnoutService).
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final BurnoutService burnoutService;
    private final JwtService     jwtService;

    /**
     * GET /api/admin/overview
     * Returns institution-wide summary statistics.
     * All figures are aggregated — no individual records are included.
     */
    @GetMapping("/overview")
    public ResponseEntity<?> overview(@RequestHeader("Authorization") String auth) {
        jwtService.assertRole(auth, "ADMIN");

        Map<String, Object> resp = new HashMap<>();

        // In production: COUNT DISTINCT userId with checkin in last 7 days
        resp.put("totalActive", 847);

        // In production: AVG(totalScore) WHERE scoreDate >= last 7 days
        resp.put("avgScore", 9.4);

        // In production: COUNT departments with AVG score > 11
        resp.put("highRiskDepts", 3);

        // In production: COUNT anonymised users with 3+ consecutive CRITICAL (no names)
        resp.put("criticalAlerts", 12);

        // Dept breakdown — aggregated, never individual
        resp.put("departments", new Object[]{
            Map.of("name", "Emergency Medicine",  "level", "CRITICAL",  "avg", 14.2),
            Map.of("name", "ICU / Critical Care", "level", "HIGH",      "avg", 12.6),
            Map.of("name", "Surgery",             "level", "HIGH",      "avg", 11.9),
            Map.of("name", "Internal Medicine",   "level", "MODERATE",  "avg",  8.7),
            Map.of("name", "Radiology",           "level", "LOW",       "avg",  5.2),
        });

        resp.put("roleBreakdown", new Object[]{
            Map.of("role", "Intern",           "level", "HIGH",     "avg", 12.4),
            Map.of("role", "Junior Resident",  "level", "CRITICAL", "avg", 13.8),
            Map.of("role", "Senior Resident",  "level", "HIGH",     "avg", 11.2),
            Map.of("role", "Consultant",       "level", "MODERATE", "avg",  7.6),
        });

        resp.put("note",
            "All data is anonymised aggregate. No individual doctor records are exposed.");
        return ResponseEntity.ok(resp);
    }

    /**
     * GET /api/admin/departments/{dept}
     * Returns aggregate burnout data for a specific department.
     *
     * @param dept        URL-encoded department name
     * @param institution institution identifier
     */
    @GetMapping("/departments/{dept}")
    public ResponseEntity<?> getDeptDetail(
        @RequestHeader("Authorization") String auth,
        @PathVariable String dept,
        @RequestParam String institution
    ) {
        jwtService.assertRole(auth, "ADMIN");

        Map<String, Object> data = new HashMap<>(burnoutService.getDeptAggregate(institution, dept));
        data.put("note",
            "Aggregate only. Minimum 5 responses required before any data is shown.");
        data.put("department",  dept);
        data.put("institution", institution);
        return ResponseEntity.ok(data);
    }

    /**
     * GET /api/admin/trends
     * Returns 7-day institution-wide trend data for charting.
     * Returns day labels + average scores only — no individual breakdown.
     */
    @GetMapping("/trends")
    public ResponseEntity<?> getTrends(@RequestHeader("Authorization") String auth) {
        jwtService.assertRole(auth, "ADMIN");

        // In production: query AVG(totalScore) GROUP BY scoreDate for last 7 days
        return ResponseEntity.ok(Map.of(
            "days",   new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"},
            "scores", new double[]{8.2, 9.1, 10.3, 9.8, 10.1, 8.7, 9.4},
            "note",   "Department-wide averages. No individual data is shown or stored."
        ));
    }
}
