package com.project.Messenger.backend.data.repository;

import com.project.Messenger.backend.data.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findRoomByRoomId(long roomId);
    List<Room> findAll();
    boolean existsRoomByRoomId(long roomId);
    void deleteByRoomId(long roomId);
}
