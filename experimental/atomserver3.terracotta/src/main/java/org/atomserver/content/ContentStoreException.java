package org.atomserver.content;

public class ContentStoreException extends Exception {
    public ContentStoreException() {
    }

    public ContentStoreException(String message) {
        super(message);
    }

    public ContentStoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContentStoreException(Throwable cause) {
        super(cause);
    }
}
