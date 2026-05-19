package ru.agluzhin.exception;

public class UserNotFoundException extends Exception {

    public UserNotFoundException(long id) {
        super("User not found: id=" + id);
    }

}
