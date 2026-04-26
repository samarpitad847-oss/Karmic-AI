package com.karmicai.service;

import com.karmicai.model.PeerRoom;
import com.karmicai.repository.PeerRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * PeerService manages peer room state and in-memory ephemeral messages.
 *
 * PRIVACY DESIGN:
 * - Messages are stored in-memory only (ConcurrentHashMap), never persisted.
 * - Each message is linked only to the sender's anonymousId — never userId or email.
 * - The message list is bounded (MAX_MESSAGES_PER_ROOM) and auto-evicted after 24h.
 * - Room membership is tracked by anonymousId only.
 */
@Service
@RequiredArgsConstructor
public class PeerService {

    private final PeerRoomRepository peerRoomRepo;

    // In-memory ephemeral message store: roomId → list of message maps
    private final Map<String, CopyOnWriteArrayList<Map<String, Object>>> messageStore
        = new ConcurrentHashMap<>();

    // In-memory active session tracker: roomId → set of anonymousIds currently online
    private final Map<String, CopyOnWriteArrayList<String>> roomSessions
        = new ConcurrentHashMap<>();

    private static final int MAX_MESSAGES_PER_ROOM = 200;

    // ── ROOMS ────────────────────────────────────────────────────────────────

    /**
     * Returns all visible rooms for a given user context.
     * Women-only rooms are excluded unless womenSafeMode is true.
     */
    public List<PeerRoom> getRoomsForUser(boolean womenSafeMode) {
        if (womenSafeMode) {
            return peerRoomRepo.findAll();
        }
        return peerRoomRepo.findByWomenOnlyFalse();
    }

    /**
     * Returns rooms filtered by category.
     */
    public List<PeerRoom> getRoomsByCategory(String category, boolean womenSafeMode) {
        List<PeerRoom> rooms = peerRoomRepo.findByCategory(category);
        if (!womenSafeMode) {
            rooms.removeIf(PeerRoom::isWomenOnly);
        }
        return rooms;
    }

    /**
     * Get a single room by ID.
     */
    public Optional<PeerRoom> getRoom(String roomId) {
        return peerRoomRepo.findById(roomId);
    }

    // ── JOIN / LEAVE ─────────────────────────────────────────────────────────

    /**
     * Register a user session joining a room.
     * Only anonymousId is tracked — no real identity.
     *
     * @return current online count for the room
     */
    public int joinRoom(String roomId, String anonymousId) {
        roomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>());
        CopyOnWriteArrayList<String> sessions = roomSessions.get(roomId);
        if (!sessions.contains(anonymousId)) {
            sessions.add(anonymousId);
        }

        // Update persisted online count approximation
        peerRoomRepo.findById(roomId).ifPresent(r -> {
            r.setOnlineCount(sessions.size());
            r.setLastActivityAt(LocalDateTime.now());
            peerRoomRepo.save(r);
        });

        return sessions.size();
    }

    /**
     * Remove a user session from a room.
     */
    public int leaveRoom(String roomId, String anonymousId) {
        CopyOnWriteArrayList<String> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            sessions.remove(anonymousId);
            int count = sessions.size();
            peerRoomRepo.findById(roomId).ifPresent(r -> {
                r.setOnlineCount(Math.max(0, count));
                peerRoomRepo.save(r);
            });
            return count;
        }
        return 0;
    }

    // ── MESSAGES ─────────────────────────────────────────────────────────────

    /**
     * Store an ephemeral message in-memory.
     *
     * PRIVACY: Only anonymousId is stored, never userId or any PII.
     * Messages auto-evict when the list exceeds MAX_MESSAGES_PER_ROOM (FIFO).
     *
     * @param roomId      target room
     * @param anonymousId sender anonymous ID (e.g. "anon-K7PR")
     * @param text        message body
     * @param isAnonymous whether sender wants to be shown as fully anon
     * @return the stored message map (safe to return to caller)
     */
    public Map<String, Object> addMessage(String roomId, String anonymousId,
                                          String text, boolean isAnonymous) {

        // Reject empty or excessively long messages
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Message cannot be empty.");
        }
        if (text.length() > 1000) {
            throw new IllegalArgumentException("Message too long (max 1000 chars).");
        }

        messageStore.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>());
        CopyOnWriteArrayList<Map<String, Object>> msgs = messageStore.get(roomId);

        String displayId = isAnonymous ? "anon" : anonymousId;
        Map<String, Object> msg = Map.of(
            "from",      displayId,
            "text",      sanitize(text),
            "timestamp", LocalDateTime.now().toString(),
            "roomId",    roomId
        );

        msgs.add(msg);

        // Evict oldest if over capacity
        while (msgs.size() > MAX_MESSAGES_PER_ROOM) {
            msgs.remove(0);
        }

        // Update last activity
        peerRoomRepo.findById(roomId).ifPresent(r -> {
            r.setLastActivityAt(LocalDateTime.now());
            peerRoomRepo.save(r);
        });

        return msg;
    }

    /**
     * Returns recent messages for a room.
     * Max 50 returned per request to keep payloads small.
     */
    public List<Map<String, Object>> getRecentMessages(String roomId, int limit) {
        CopyOnWriteArrayList<Map<String, Object>> msgs = messageStore.get(roomId);
        if (msgs == null || msgs.isEmpty()) return List.of();

        int from = Math.max(0, msgs.size() - Math.min(limit, 50));
        return List.copyOf(msgs.subList(from, msgs.size()));
    }

    /**
     * Returns current online count for a room.
     */
    public int getOnlineCount(String roomId) {
        CopyOnWriteArrayList<String> sessions = roomSessions.get(roomId);
        return sessions != null ? sessions.size() : 0;
    }

    // ── UTILS ────────────────────────────────────────────────────────────────

    /** Basic HTML-escape to prevent XSS in stored messages. */
    private String sanitize(String input) {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
