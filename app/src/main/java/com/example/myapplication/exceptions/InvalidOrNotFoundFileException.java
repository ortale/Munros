package com.example.myapplication.exceptions;

import java.io.IOException;

/**
 * Custom IOException used to present custom message for CSV file errors.
 */
public class InvalidOrNotFoundFileException extends IOException {
    public InvalidOrNotFoundFileException(String message) {
        super(message);
    }
}
