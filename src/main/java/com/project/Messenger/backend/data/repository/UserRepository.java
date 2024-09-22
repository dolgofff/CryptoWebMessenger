package com.project.Messenger.backend.data.repository;

import com.project.Messenger.backend.data.model.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

//TODO: Убедиться, что вы указали менеджер транзакций в конфигурации. Это делается в любом случае.
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findUserByUsername(String username);

    User getUserByUsername(String username);

    Optional<User> getUserByPersonId(long personId);

}
