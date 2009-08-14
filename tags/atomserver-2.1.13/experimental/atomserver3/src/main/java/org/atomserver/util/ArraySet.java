package org.atomserver.util;

import java.util.*;

/**
 * an implementation of Set that uses an array to back the collection.
 * <p/>
 * This Set is useful for situations where set semantics are desired, but the sets will be
 * extremely small (generally < 10 elements) such that space and time can be greatly optimized by
 * storing the set elements in an array, and iterating through the array to determine set
 * containment.
 *
 * For general use, this set would not be a good choice - all operations are O(N) in the size of
 * the set - add and remove operations allocate a new array and copy the elements of the set into
 * the new array.  But when you are dealing with many sets like this one, you know that the sets
 * are going to be small and not change terribly often, and you want to maximize space usage, this
 * allows you to use the friendly semantics of java.util.Set with the space constraints of a
 * single array containing the elements, plus four bytes for the object pointer.
 */
public class ArraySet<T> extends AbstractSet<T> {

    private static final Object[] EMPTY = new Object[0];

    private Object[] values = EMPTY;

    public ArraySet() {
        this.values = EMPTY;
    }

    public <U extends T> ArraySet(U[] array) {
        values = new Object[array.length];
        System.arraycopy(array, 0, values, 0, array.length);
        uniquify();
    }

    public ArraySet(Collection<? extends T> collection) {
        collection.toArray(values = new Object[collection.size()]);
        uniquify();
    }

    private void uniquify() {
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++ ) {
                if (values[i].equals(values[j])) {
                    deleteAt(j--);
                }
            }
        }
    }

    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int index = 0;
            boolean canDelete = false;

            public boolean hasNext() {
                return index < values.length;
            }

            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(
                            "no more elements");
                }
                canDelete = true;
                return (T) values[index++];
            }

            public void remove() {
                if (canDelete) {
                    deleteAt(--index);
                    canDelete = false;
                } else {
                    throw new IllegalStateException(
                            "remove called without corresponding call to next");
                }
            }
        };
    }

    public boolean contains(Object o) {
        for (Object object : values) {
            if (o.equals(object)) {
                return true;
            }
        }
        return false;
    }

    public boolean add(T t) {
        if (contains(t)) {
            return false;
        }
        Object[] newValues = new Object[this.values.length + 1];
        System.arraycopy(this.values, 0, newValues, 0, this.values.length);
        newValues[newValues.length - 1] = t;
        this.values = newValues;
        return true;
    }

    public boolean remove(Object o) {
        for (int i = 0; i < this.values.length; i++) {
            if (o.equals(this.values[i])) {
                deleteAt(i);
                return true;
            }
        }
        return false;
    }

    public void clear() {
        this.values = EMPTY;
    }

    void deleteAt(int index) {
        Object[] newValues = new Object[values.length - 1];
        System.arraycopy(this.values, 0, newValues, 0, index);
        System.arraycopy(this.values, index + 1, newValues, index, this.values.length - index - 1);
        this.values = newValues;
    }

    public int size() {
        return values.length;
    }
}
