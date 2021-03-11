package com.example.myapplication.exceptions;

/**
 * Exception used in case of invalid queries on search.
 */
public class InvalidQueryException extends Exception {
    public InvalidQueryException(String message) {
        super(message);
    }
}
