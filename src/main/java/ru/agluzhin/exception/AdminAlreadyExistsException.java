package ru.agluzhin.exception;

public class AdminAlreadyExistsException extends Exception {

    public AdminAlreadyExistsException() {
        super("Administrator already exists. Only one admin is allowed.");
    }

}
