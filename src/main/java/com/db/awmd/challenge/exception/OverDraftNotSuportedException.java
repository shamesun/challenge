package com.db.awmd.challenge.exception;

public class OverDraftNotSuportedException extends RuntimeException {
    public OverDraftNotSuportedException(String message) {
        super(message);
    }
}
