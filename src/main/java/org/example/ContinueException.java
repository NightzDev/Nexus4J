package org.example;

class ContinueException extends RuntimeException {
    ContinueException() {
        super(null, null, false, false);
    }
}
