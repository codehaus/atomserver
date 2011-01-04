package org.atomserver.content;

public class ContentStoreException extends Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = 2884360818074957385L;

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
