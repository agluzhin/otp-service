package ru.agluzhin.exception;

public class InvalidOtpException extends Exception {

    public InvalidOtpException(String reason) {
        super("OTP validation failed: " + reason);
    }

}
