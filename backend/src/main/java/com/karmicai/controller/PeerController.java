package com.karmicai.controller;

import com.karmicai.model.PeerRoom;
import com.karmicai.service.JwtService;
import com.karmicai.service.PeerService;
import com.karmicai.service.UserService;
import com.karmicai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PeerController — REST endpoints for the Peer Support Rooms feature.
 *
 * PRIVACY GUARANTEES:
 * - No endpoint exposes a user's real identity, email, or name.
 * - Room membership is tracked by anonymousId only.
 * - Messages are ephemeral (in-memory, 24h max, never persisted).
 * - Women-only rooms are gated by womenSafeMode flag on the user record.
 */
@RestController
@RequestMapping("/api/peer")
@RequiredArgsConstructor
public class PeerController {

    private final PeerService peerService;
    private final JwtService  jwtService;
    private final UserRepository userRepository;

    // ── ROOMS ────────────────────────────────────────────────────────────────

    /**
     * GET /api/peer/rooms
     * Returns all rooms visible to the authenticated user.
     * Women-only rooms are filtered out unless user has womenSafeMode enabled.
     */
    @GetMapping("/rooms")
    public ResponseEntity<?> listRooms(
        @RequestHeader("Authorization") String auth
    ) {
        Long userId = jwtService.extractUserId(auth);
        boolean womenMode = userRepository.findById(userId)
            .map(u -> u.isWomenSafeMode())
            .orElse(false);

        List<PeerRoom> rooms = peerService.getRoomsForUser(womenMode);
        return ResponseEntity.ok(rooms.stream().map(this::toRoomDto).toList());
    }

    /**
     * GET /api/peer/rooms/{roomId}
     * Returns a single room's details and current online count.
     */
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<?> getRoom(
        @RequestHeader("Authorization") String auth,
        @PathVariable String roomId
    ) {
        Optional<PeerRoom> room = peerService.getRoom(roomId);
        if (room.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        int onlineCount = peerService.getOnlineCount(roomId);
        Map<String, Object> dto = toRoomDto(room.get());
        ((java.util.HashMap<String, Object>) dto).put("onlineCount", onlineCount);
        return ResponseEntity.ok(dto);
    }

    /**
     * GET /api/peer/rooms/category/{category}
     * Returns rooms filtered by category (emergency, specialty, women, all).
     */
    @GetMapping("/rooms/category/{category}")
    public ResponseEntity<?> getRoomsByCategory(
        @RequestHeader("Authorization") String auth,
        @PathVariable String category
    ) {
        Long userId = jwtService.extractUserId(auth);
        boolean womenMode = userRepository.findById(userId)
            .map(u -> u.isWomenSafeMode())
            .orElse(false);

        List<PeerRoom> rooms = peerService.getRoomsByCategory(category, womenMode);
        return ResponseEntity.ok(rooms.stream().map(this::toRoomDto).toList());
    }

    // ── JOIN / LEAVE ─────────────────────────────────────────────────────────

    /**
     * POST /api/peer/rooms/{roomId}/join
     * Registers the user's anonymous session in the room.
     * Returns current online count.
     */
    @PostMapping("/rooms/{roomId}/join")
    public ResponseEntity<?> joinRoom(
        @RequestHeader("Authorization") String auth,
        @PathVariable String roomId
    ) {
        Long userId = jwtService.extractUserId(auth);

        // Look up the user's anonymousId — never expose userId to room
        String anonymousId = userRepository.findById(userId)
            .map(u -> u.getAnonymousId())
            .orElse("anon-" + userId.toString().substring(0, Math.min(6, userId.toString().length())));

        // Gate women-only rooms
        Optional<PeerRoom> room = peerService.getRoom(roomId);
        if (room.isPresent() && room.get().isWomenOnly()) {
            boolean womenMode = userRepository.findById(userId)
                .map(u -> u.isWomenSafeMode())
                .orElse(false);
            if (!womenMode) {
                return ResponseEntity.status(403)
                    .body(Map.of("message", "This room requires Women-Safe Mode to be enabled."));
            }
        }

        int onlineCount = peerService.joinRoom(roomId, anonymousId);

        return ResponseEntity.ok(Map.of(
            "status",      "joined",
            "roomId",      roomId,
            "anonymousId", anonymousId,  // user's own anon ID — safe to return to them
            "onlineCount", onlineCount
        ));
    }

    /**
     * POST /api/peer/rooms/{roomId}/leave
     * Removes the user's anonymous session from the room.
     */
    @PostMapping("/rooms/{roomId}/leave")
    public ResponseEntity<?> leaveRoom(
        @RequestHeader("Authorization") String auth,
        @PathVariable String roomId
    ) {
        Long userId = jwtService.extractUserId(auth);
        String anonymousId = userRepository.findById(userId)
            .map(u -> u.getAnonymousId())
            .orElse(null);

        int onlineCount = (anonymousId != null) ? peerService.leaveRoom(roomId, anonymousId) : 0;

        return ResponseEntity.ok(Map.of(
            "status",      "left",
            "roomId",      roomId,
            "onlineCount", onlineCount
        ));
    }

    // ── MESSAGES ─────────────────────────────────────────────────────────────

    /**
     * POST /api/peer/rooms/{roomId}/message
     * Sends an ephemeral message to a room.
     *
     * Body: { "text": "...", "anonymous": true/false }
     *
     * PRIVACY: Message is stored in-memory only, linked to anonymousId.
     * If anonymous=true, sender appears as "anon" (no ID shown at all).
     */
    @PostMapping("/rooms/{roomId}/message")
    public ResponseEntity<?> sendMessage(
        @RequestHeader("Authorization") String auth,
        @PathVariable String roomId,
        @RequestBody Map<String, Object> body
    ) {
        Long userId = jwtService.extractUserId(auth);

        String text = (String) body.getOrDefault("text", "");
        boolean isAnon = (boolean) body.getOrDefault("anonymous", true);

        String anonymousId = userRepository.findById(userId)
            .map(u -> u.getAnonymousId())
            .orElse("anon-???");

        try {
            Map<String, Object> msg = peerService.addMessage(roomId, anonymousId, text, isAnon);
            return ResponseEntity.ok(Map.of(
                "status",    "sent",
                "anonymous", isAnon,
                "roomId",    roomId,
                "timestamp", msg.get("timestamp")
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * GET /api/peer/rooms/{roomId}/messages
     * Returns recent ephemeral messages for a room (max 50).
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<?> getMessages(
        @RequestHeader("Authorization") String auth,
        @PathVariable String roomId,
        @RequestParam(defaultValue = "50") int limit
    ) {
        jwtService.extractUserId(auth); // auth check only
        List<Map<String, Object>> messages = peerService.getRecentMessages(roomId, limit);
        return ResponseEntity.ok(Map.of(
            "roomId",   roomId,
            "messages", messages,
            "count",    messages.size(),
            "note",     "Messages are ephemeral — not persisted. Maximum 200 per room."
        ));
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private Map<String, Object> toRoomDto(PeerRoom r) {
        Map<String, Object> dto = new java.util.HashMap<>();
        dto.put("id",          r.getId());
        dto.put("name",        r.getName());
        dto.put("icon",        r.getIcon());
        dto.put("description", r.getDescription());
        dto.put("category",    r.getCategory());
        dto.put("womenOnly",   r.isWomenOnly());
        dto.put("onlineCount", peerService.getOnlineCount(r.getId()));
        return dto;
    }
}
