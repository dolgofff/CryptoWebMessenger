package com.project.Messenger.backend.data.service;

import com.project.Messenger.backend.data.model.User;
import com.project.Messenger.backend.data.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@AllArgsConstructor
@Service
public class RegisterService {

    public static class RegisterException extends Exception {
    }

    private final UserService userService;

    public void register(String username, String password) throws RegisterException {
        Optional<User> getUser = userService.findUserByUsername(username);
        if (getUser.isEmpty()) {
            userService.save(new User(username, password));
        } else {
            // "such username already exists"
            throw new RegisterException();
        }
    }
}
