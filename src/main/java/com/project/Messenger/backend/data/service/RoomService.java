package com.project.Messenger.backend.data.service;

import com.project.Messenger.backend.data.model.Room;
import com.project.Messenger.backend.data.model.User;
import com.project.Messenger.backend.data.repository.RoomRepository;
import com.project.Messenger.backend.data.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@AllArgsConstructor
public class RoomService {
    private RoomRepository roomRepository;
    private static final Random rd = new Random();

    public long prepareRoom(long ownerId, long guestId, String cipher, String cipherMode, String padding,
                            byte[] p, byte[] g) {

        long generatedId = generateRoomId(ownerId, guestId);
        int blockSizeInBits;
        byte[] initializationVector;

        if (cipher.equals("RC5")) {
            blockSizeInBits = 64;
            initializationVector = generateIV(8);
        } else {
            blockSizeInBits = 128;
            initializationVector = generateIV(16);
        }

        Room room = Room.builder().
                ownerId(ownerId).
                guestId(guestId).
                roomId(generatedId).
                cipher(cipher).
                cipherMode(cipherMode).
                padding(padding).
                blockSizeInBits(blockSizeInBits).
                initializationVector(initializationVector).
                p(p).g(g).build();


        roomRepository.save(room);

        return generatedId;
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public void deleteRoom(Long id) {
        roomRepository.deleteByRoomId(id);
    }


    public Optional<Room> getRoomById(Long roomId) {
        return roomRepository.findById(roomId);
    }

    public Optional<Room> getRoomByRoomId(long roomId) {
        return roomRepository.findRoomByRoomId(roomId);
    }


    public void addUserToRoom(User user, Room room) {
        room.getConnectedUsers().add(user);
        roomRepository.save(room);
    }

    public boolean removeUserFromRoom(User user, Room room) {
        if (room.getConnectedUsers().contains(user)) {
            room.getConnectedUsers().remove(user);
            roomRepository.save(room);
            return true;
        }
        return false;
    }

    private long generateRoomId(long id1, long id2) {
        // Упорядочиваем ID, чтобы избежать зависимости от порядка
        String combinedId = id1 < id2 ? id1 + "-" + id2 : id2 + "-" + id1;

        // Вычисляем хеш
        return combinedId.hashCode();
    }

    @Transactional
    public boolean isBothActive(long roomId) {
        Optional<Room> getRoom = roomRepository.findRoomByRoomId(roomId);
        if(getRoom.isPresent()){
            Room room = getRoom.get();
            List<User> connectedUsers = room.getConnectedUsers();
            return connectedUsers.size() == 2;  // Проверка, что два пользователя подключены
        }
        return false;
    }

    private byte[] generateIV(int size) {
        byte[] vector = new byte[size];

        for (int i = 0; i < size; i++) {
            vector[i] = (byte) rd.nextInt(128);
        }

        return vector;
    }
}
