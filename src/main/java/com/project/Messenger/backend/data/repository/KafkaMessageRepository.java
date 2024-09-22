package com.project.Messenger.backend.data.repository;

import com.project.Messenger.backend.broker.KafkaMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KafkaMessageRepository extends JpaRepository<KafkaMessage, Long> {

    public List<KafkaMessage> findAllByRoomId(long roomId);

    public void deleteByIdAndRoomId(long id, long roomId);

    public void deleteAllByRoomId(long roomId);
}
