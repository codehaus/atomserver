package org.atomserver.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SubtractIterator<T extends Comparable<T>> implements Iterator<T> {

    private final Iterator<T> a;
    private final Iterator<T> b;
    private T aNext;
    private T bNext;

    public SubtractIterator(Iterator<T> a, Iterator<T> b) {
        this.a = a;
        this.b = b;
        bNext = b.hasNext() ? b.next() : null;
        advance();
    }

    public boolean hasNext() { return aNext != null; }

    public T next() {
        if (!hasNext()) { throw new NoSuchElementException(); }
        T next = aNext;
        advance();
        return next;
    }

    private void advance() {
        aNext = a.hasNext() ? a.next() : null;
        while (aNext != null && bNext != null && bNext.compareTo(aNext) <= 0) {
            while (bNext != null && bNext.compareTo(aNext) < 0) {
                bNext = b.hasNext() ? b.next() : null;
            }
            if (bNext != null && aNext.equals(bNext)) {
                aNext = a.hasNext() ? a.next() : null;
            }
        }
    }

    public void remove() { throw new UnsupportedOperationException(); }
}