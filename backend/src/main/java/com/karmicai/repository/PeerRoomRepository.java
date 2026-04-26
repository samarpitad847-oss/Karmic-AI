package com.karmicai.repository;

import com.karmicai.model.PeerRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PeerRoomRepository extends JpaRepository<PeerRoom, String> {
    List<PeerRoom> findByCategory(String category);
    List<PeerRoom> findByWomenOnlyFalse();
    List<PeerRoom> findByWomenOnlyFalseOrWomenOnly(boolean womenOnly);
}
