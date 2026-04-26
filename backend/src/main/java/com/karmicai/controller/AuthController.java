package com.karmicai.controller;

import com.karmicai.model.User;
import com.karmicai.model.User.Gender;
import com.karmicai.model.User.Role;
import com.karmicai.service.JwtService;
import com.karmicai.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService  jwtService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> body) {
        try {
            Role role = Role.valueOf(body.getOrDefault("role", "JUNIOR_RESIDENT"));
            Gender gender = parseGender(body.get("gender"));

            User user = userService.register(
                body.get("email"), body.get("password"), role,
                body.get("institution"), body.get("department"), gender
            );

            String token = jwtService.generateToken(user.getId(), user.getRole().name());
            return ResponseEntity.ok(Map.of(
                "token",        token,
                "role",         user.getRole().name(),
                "anonymousId",  user.getAnonymousId(),
                "gender",       gender != null ? gender.name() : "",
                "womenSafeMode", user.isWomenSafeMode()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("message", "Signup failed. Please try again."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            User user = userService.authenticate(body.get("email"), body.get("password"));
            String token = jwtService.generateToken(user.getId(), user.getRole().name());

            boolean isAdmin = user.getRole() == Role.ADMIN;
            return ResponseEntity.ok(Map.of(
                "token",        token,
                "role",         user.getRole().name(),
                "anonymousId",  user.getAnonymousId(),
                "gender",       user.getGender() != null ? user.getGender().name() : "",
                "womenSafeMode", user.isWomenSafeMode(),
                "redirectTo",   isAdmin ? "admin.html" : "dashboard.html"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private Gender parseGender(String val) {
        try { return val != null ? Gender.valueOf(val) : null; }
        catch (Exception e) { return null; }
    }
}
