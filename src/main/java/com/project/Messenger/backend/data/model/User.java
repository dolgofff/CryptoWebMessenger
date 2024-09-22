package com.project.Messenger.backend.data.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;

@Entity
@Table(name = "Users")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @NotEmpty(message = "Username must not be empty!")
    String username;
    String passwordSalt;
    String passwordHash;

    private long[] connectedRooms;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long personId;

    public User(String username, String password) {
        this.username = username;
        this.passwordSalt = RandomStringUtils.random(32);
        this.passwordHash = DigestUtils.sha1Hex(password + passwordSalt);
    }


    //Для входа в мессенджер
    public boolean checkPassword(String password){
        return DigestUtils.sha1Hex(password + passwordSalt).equals(passwordHash);
    }


}
