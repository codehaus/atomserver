package org.atomserver.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class UnionIterator<T extends Comparable<T>> implements Iterator<T> {

    private final Iterator<T> left;
    private final Iterator<T> right;
    private T leftNext = null;
    private T rightNext = null;

    public UnionIterator(Iterator<T> left, Iterator<T> right) {
        this.left = left;
        this.right = right;
        this.leftNext = this.left.hasNext() ? this.left.next() : null;
        this.rightNext = this.right.hasNext() ? this.right.next() : null;
    }

    public boolean hasNext() {
        return this.leftNext != null || this.rightNext != null;
    }

    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        T next;
        if (this.rightNext == null ||
            (this.leftNext != null && this.leftNext.compareTo(this.rightNext) < 0)) {
            next = this.leftNext;
            this.leftNext = this.left.hasNext() ? this.left.next() : null;
        } else if (this.leftNext == null || this.rightNext.compareTo(this.leftNext) < 0) {
            next = this.rightNext;
            this.rightNext = this.right.hasNext() ? this.right.next() : null;
        } else {
            next = this.leftNext;
            this.leftNext = this.left.hasNext() ? this.left.next() : null;
            this.rightNext = this.right.hasNext() ? this.right.next() : null;
        }
        return next;
    }

    public void remove() {
        throw new UnsupportedOperationException(
                "remove is not supported - this iterator is read-only");
    }
}
