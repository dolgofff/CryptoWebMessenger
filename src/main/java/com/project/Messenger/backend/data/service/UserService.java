package com.project.Messenger.backend.data.service;

import com.project.Messenger.backend.data.model.User;
import com.project.Messenger.backend.data.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class UserService {
    private UserRepository userRepository;

    public void save(User user) {
        userRepository.save(user);
    }

    public Optional<User> findUserByUsername(String username) {
        return userRepository.findUserByUsername(username);
    }

    public User getUserByUsername(String username) {
        return userRepository.getUserByUsername(username);
    }

    public boolean checkIfExists(long personId) {
        return userRepository.getUserByPersonId(personId).isPresent();
    }

    public Optional<User> getUserByPersonId(long personId) {
        return userRepository.getUserByPersonId(personId);
    }

    @Transactional
    public User joinRoom(long personId, long roomId) {
        Optional<User> getUser = userRepository.getUserByPersonId(personId);
        if (getUser.isPresent()) {
            User user = getUser.get();
            int currentRoomsAmount = (user.getConnectedRooms() != null) ? user.getConnectedRooms().length : 0;
            long[] currentRoomsList = (user.getConnectedRooms() != null) ? user.getConnectedRooms() : new long[0];
            long[] joinedRoomsList = new long[currentRoomsAmount + 1];

            System.arraycopy(currentRoomsList, 0, joinedRoomsList, 0, currentRoomsAmount);
            joinedRoomsList[joinedRoomsList.length - 1] = roomId;

            user.setConnectedRooms(joinedRoomsList);

            //save работает как update
            return userRepository.save(user);
        }
        return null;
    }

    @Transactional
    public User leaveRoom(long personId, long roomId) {
        Optional<User> getUser = userRepository.getUserByPersonId(personId);
        if (getUser.isPresent()) {
            User user = getUser.get();
            long[] currentRooms = user.getConnectedRooms();
            long[] newValue = new long[currentRooms.length - 1];
            int i = 0;
            for (long r : currentRooms) {
                if (r != roomId) {
                    newValue[i++] = r;
                }
            }
            user.setConnectedRooms(newValue);

            return userRepository.save(user);
        }
        return null;
    }
}
