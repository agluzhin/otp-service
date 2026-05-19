package ru.agluzhin.exception;

public class UserAlreadyExistsException extends Exception {

    public UserAlreadyExistsException(String login) {
        super("User already exists: " + login);
    }

}
