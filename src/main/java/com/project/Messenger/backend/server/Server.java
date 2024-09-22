package com.project.Messenger.backend.server;

import com.project.Messenger.backend.data.model.Room;
import com.project.Messenger.backend.data.model.User;
import com.project.Messenger.backend.data.service.RoomService;
import com.project.Messenger.backend.data.service.UserService;
import com.project.Messenger.backend.encryption.dhProtocol.DiffieHellmanProtocol;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.*;

@Service
@Slf4j
public class Server {
    private final UserService userService;
    private final RoomService roomService;

    public Server(UserService userService, RoomService roomService) {
        this.userService = userService;
        this.roomService = roomService;
    }

    @Transactional
    public synchronized long createRoom(long ownerId, long guestId, String cipher, String cipherMode, String padding) {
        int bitLength;

        if (cipher.equals("RC5")) {
            bitLength = 64;
        } else {
            bitLength = 128;
        }

        BigInteger[] dHNumbers = DiffieHellmanProtocol.generationEngine(bitLength);
        byte[] p = dHNumbers[0].toByteArray();
        byte[] g = dHNumbers[1].toByteArray();

        long generatedId = roomService.prepareRoom(ownerId, guestId, cipher, cipherMode, padding, p, g);

        userService.joinRoom(ownerId, generatedId);
        userService.joinRoom(guestId, generatedId);

        return generatedId;
    }

    @Transactional
    public synchronized boolean deleteRoom(long roomId) {
        Optional<Room> getRoom = roomService.getRoomByRoomId(roomId);

        if (getRoom.isPresent()) {
            Room roomToDelete = getRoom.get();
            List<User> connectedUsers = roomToDelete.getConnectedUsers();

            for (User u : connectedUsers) {
                if (!disconnectRoom(u.getPersonId(), roomToDelete.getRoomId())) {
                    return false;
                }
            }

            userService.leaveRoom(roomToDelete.getOwnerId(), roomId);
            userService.leaveRoom(roomToDelete.getGuestId(), roomId);
            roomService.deleteRoom(roomToDelete.getRoomId());

            return true;
        }
        return false;
    }

    @Transactional
    public synchronized boolean disconnectRoom(long userId, long roomId) {
        Optional<Room> getRoom = roomService.getRoomByRoomId(roomId);
        Optional<User> getUser = userService.getUserByPersonId(userId);

        if (getRoom.isPresent() && getUser.isPresent()) {
            Room roomToLeave = getRoom.get();
            User userToDisconnect = getUser.get();

            roomService.removeUserFromRoom(userToDisconnect, roomToLeave);

            return true;
        }
        return false;
    }

    @Transactional
    public synchronized boolean connectRoom(long userId, long roomId) {
        Optional<Room> getRoom = roomService.getRoomByRoomId(roomId);
        Optional<User> getUser = userService.getUserByPersonId(userId);

        if (getRoom.isPresent() && getUser.isPresent()) {
            Room roomToConnect = getRoom.get();
            User user = getUser.get();

            if (!(roomToConnect.getConnectedUsers().contains(user))) {
                if (roomToConnect.getConnectedUsers().size() != 2) {
                    roomService.addUserToRoom(user, roomToConnect);
                    return true;
                }
            }
        }
        return false;
    }
}
