package ru.agluzhin.exception;

public class InvalidCredentialsException extends Exception {

    public InvalidCredentialsException() {
        super("Invalid login or password");
    }

}
