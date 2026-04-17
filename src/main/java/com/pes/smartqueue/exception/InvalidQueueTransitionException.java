package com.pes.smartqueue.exception;

public class InvalidQueueTransitionException extends RuntimeException {
    public InvalidQueueTransitionException(String message) {
        super(message);
    }
}
