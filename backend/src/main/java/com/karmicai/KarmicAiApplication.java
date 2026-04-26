package com.karmicai;

import com.karmicai.model.PeerRoom;
import com.karmicai.repository.PeerRoomRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class KarmicAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(KarmicAiApplication.class, args);
    }

    /**
     * Seed default peer rooms on startup if they don't exist yet.
     * Uses H2 in-memory DB for development; swap to PostgreSQL for production.
     */
    @Bean
    CommandLineRunner seedData(PeerRoomRepository peerRoomRepo) {
        return args -> {
            if (peerRoomRepo.count() == 0) {
                List<PeerRoom> rooms = PeerRoom.defaultRooms();
                peerRoomRepo.saveAll(rooms);
                System.out.println("[Karmic AI] Seeded " + rooms.size() + " peer rooms.");
            }
            System.out.println("[Karmic AI] Application started — Zero-Knowledge Privacy Mode active.");
        };
    }
}
