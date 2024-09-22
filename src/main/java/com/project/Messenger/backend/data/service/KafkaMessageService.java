package com.project.Messenger.backend.data.service;

import com.project.Messenger.backend.broker.KafkaMessage;
import com.project.Messenger.backend.data.repository.KafkaMessageRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class KafkaMessageService {

    private KafkaMessageRepository kafkaMessageRepository;

    public List<KafkaMessage> getMessagesByRoomId(long roomId) {
        return kafkaMessageRepository.findAllByRoomId(roomId);
    }

    @Transactional
    public void deleteMessageById(long id, long roomId) {
        kafkaMessageRepository.deleteByIdAndRoomId(id, roomId);
    }

    @Transactional
    public void deleteAllMessagesByRoomId(long roomId) {
        kafkaMessageRepository.deleteAllByRoomId(roomId);
    }

    @Transactional
    public KafkaMessage save(KafkaMessage kafkaMessage) {
        return kafkaMessageRepository.save(kafkaMessage);
    }
}
