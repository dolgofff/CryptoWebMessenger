package com.project.Messenger.backend.data.service;

import com.project.Messenger.backend.data.model.User;
import com.project.Messenger.backend.data.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@AllArgsConstructor
@Service
public class AuthService {

    public static class AuthException extends Exception {
    }

    public static class InvalidPasswordException extends AuthException {
    }

    public static class InvalidUserException extends AuthException {
    }
    private final UserService userService;

    public long authenticate(String username, String password) throws AuthException {
        // Валидирую при регистрации так, чтобы не могло быть одинаковых username'ов
        User user = userService.getUserByUsername(username);
        if (user != null && user.checkPassword(password)) {
        } else if (user != null && !user.checkPassword(password)) {
            throw new InvalidPasswordException();
        } else if (user == null){
            throw new InvalidUserException();
        } else{
            throw new AuthException();
        }

        return user.getPersonId();
    }
}
