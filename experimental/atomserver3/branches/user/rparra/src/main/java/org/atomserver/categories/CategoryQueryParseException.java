package org.atomserver.categories;

import java.util.NoSuchElementException;

public class CategoryQueryParseException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = -3294199766453576639L;

	public CategoryQueryParseException(NoSuchElementException e) {
        super(e.getMessage(), e);
    }
}
