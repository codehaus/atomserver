package org.atomserver.categories;

import java.util.NoSuchElementException;

public class CategoryQueryParseException extends Exception {
    public CategoryQueryParseException(NoSuchElementException e) {
        super(e.getMessage(), e);
    }
}
