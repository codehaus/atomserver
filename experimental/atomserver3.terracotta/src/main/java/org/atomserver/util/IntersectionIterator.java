package org.atomserver.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class IntersectionIterator<T extends Comparable<T>> implements Iterator<T> {

    private final Iterator<T> left;
    private final Iterator<T> right;
    private T next = null;

    public IntersectionIterator(Iterator<T> left, Iterator<T> right) {
        this.left = left;
        this.right = right;
        advance();
    }

    public boolean hasNext() {
        return this.next != null;
    }

    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        T next = this.next;
        advance();
        return next;
    }

    private void advance() {
        this.next = this.left.hasNext() ? this.left.next() : null;
        T rightNext = this.right.hasNext() ? this.right.next() : null;
        while (this.next != null && rightNext != null && this.next.compareTo(rightNext) != 0) {
            while (this.next != null && this.next.compareTo(rightNext) < 0) {
                this.next = this.left.hasNext() ? this.left.next() : null;
            }
            if (this.next == null) {
                break;
            }
            while (rightNext != null && rightNext.compareTo(this.next) < 0) {
                rightNext = this.right.hasNext() ? this.right.next() : null;
            }
        }
        if (rightNext == null) {
            this.next = null;
        }
    }

    public void remove() {
        throw new UnsupportedOperationException(
                "remove is not supported - this iterator is read-only");
    }
}