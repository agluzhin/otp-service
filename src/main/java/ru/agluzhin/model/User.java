package ru.agluzhin.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {

    private long id;
    private String login;
    private String passwordHash;
    private Role role;

    public User() {}

    public User(long id, String login, String passwordHash, Role role) {
        this.id = id;
        this.login = login;
        this.passwordHash = passwordHash;
        this.role = role;
    }

}
