package org.atomserver.util;

import static junit.framework.Assert.*;

import java.util.Arrays;
import static java.util.Collections.EMPTY_SET;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

public class IteratorTest {
    private static final List<Integer> ODDS =
            Arrays.asList(1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21);
    private static final List<Integer> EVENS =
            Arrays.asList(0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20);
    private static final List<Integer> PRIMES =
            Arrays.asList(2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31);
    private static final List<Integer> TWELVE =
            Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

    @Test
    public void testUnionIterator() throws Exception {
        checkValues(new UnionIterator<Integer>(ODDS.iterator(), EVENS.iterator()),
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21);
        checkValues(new UnionIterator<Integer>(EVENS.iterator(), EMPTY_SET.iterator()),
                    0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20);
        checkValues(new UnionIterator<Integer>(EVENS.iterator(), EVENS.iterator()),
                    0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20);
        checkValues(new UnionIterator<Integer>(EMPTY_SET.iterator(), EMPTY_SET.iterator()));
    }

    @Test
    public void testIntersectionIterator() throws Exception {
        checkValues(new IntersectionIterator<Integer>(ODDS.iterator(), EVENS.iterator()));
        checkValues(new IntersectionIterator<Integer>(ODDS.iterator(), ODDS.iterator()),
                    1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21);
        checkValues(new IntersectionIterator<Integer>(ODDS.iterator(), TWELVE.iterator()),
                    1, 3, 5, 7, 9, 11);
        checkValues(new IntersectionIterator<Integer>(EMPTY_SET.iterator(), EMPTY_SET.iterator()));
    }

    @Test
    public void testSubtractIterator() throws Exception {
        checkValues(new SubtractIterator<Integer>(ODDS.iterator(), PRIMES.iterator()),
                    1, 9, 15, 21);
        checkValues(new SubtractIterator<Integer>(ODDS.iterator(), ODDS.iterator()));
        checkValues(new SubtractIterator<Integer>(TWELVE.iterator(), EMPTY_SET.iterator()),
                    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        checkValues(new SubtractIterator<Integer>(EMPTY_SET.iterator(), EMPTY_SET.iterator()));
    }

    private void checkValues(Iterator<Integer> iterator, Integer... values) {
        for (Integer expected : values) {
            assertTrue(iterator.hasNext());
            assertEquals(expected, iterator.next());
        }
        assertFalse(iterator.hasNext());
    }
}
