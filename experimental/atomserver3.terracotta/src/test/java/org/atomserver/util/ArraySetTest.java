package org.atomserver.util;

import static junit.framework.Assert.*;
import org.junit.Test;

import java.util.Set;
import java.util.Iterator;

public class ArraySetTest {

    @Test
    public void testArraySet() throws Exception {
        Set<String> strings = new ArraySet<String>();
        checkSet(strings);

        strings.add("A");
        checkSet(strings, "A");

        strings.add("B");
        checkSet(strings, "A", "B");

        strings.add("B");
        checkSet(strings, "A", "B");

        strings.add("C");
        checkSet(strings, "A", "B", "C");

        strings.remove("B");
        checkSet(strings, "A", "C");
    }

    private void checkSet(Set<String> strings, String... values) {
        if (values == null) {
            values = new String[0];
        }
        assertEquals(values.length, strings.size());
        assertEquals(values.length == 0, strings.isEmpty());
        for (String value : values) {
            assertTrue(strings.contains(value));
        }

        Set<String> copy = new ArraySet<String>();
        copy.addAll(strings);
        assertEquals(values.length, copy.size());
        assertEquals(values.length == 0, copy.isEmpty());
        for (String value : values) {
            assertTrue(copy.contains(value));
        }

        Iterator<String> stringIterator = copy.iterator();
        while (stringIterator.hasNext()) {
            String s = stringIterator.next();
            assertTrue(strings.contains(s));
            assertTrue(copy.contains(s));
            stringIterator.remove();
            assertFalse(copy.contains(s));
        }

        assertTrue(copy.isEmpty());

        copy.addAll(strings);

        assertTrue(strings.containsAll(copy));
        assertTrue(copy.containsAll(strings));

        copy.removeAll(strings);

        assertTrue(copy.isEmpty());

        copy.addAll(strings);

        copy.add("Z");
        assertFalse(strings.contains("Z"));
        assertTrue(copy.contains("Z"));
        assertTrue(copy.containsAll(strings));
        assertFalse(strings.containsAll(copy));

        copy.retainAll(strings);

        assertTrue(strings.containsAll(copy));
        assertTrue(copy.containsAll(strings));
        assertEquals(copy, strings);

        stringIterator = copy.iterator();
        try {
            stringIterator.remove();
            fail();
        } catch (IllegalStateException e) {
            // expected.
        }

        if (stringIterator.hasNext()) {
            stringIterator.next();
            stringIterator.remove();
            try {
                stringIterator.remove();
                fail();
            } catch (IllegalStateException e) {
                // expected.
            }
        }
    }
}
