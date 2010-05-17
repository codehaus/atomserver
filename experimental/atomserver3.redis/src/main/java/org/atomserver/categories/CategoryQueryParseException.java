package org.atomserver.categories;

import java.util.NoSuchElementException;

public class CategoryQueryParseException extends RuntimeException {
    public CategoryQueryParseException(NoSuchElementException e) {
        super(e.getMessage(), e);
    }
}
