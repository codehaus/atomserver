package org.atomserver.util;

import static junit.framework.Assert.*;
import org.junit.Test;

public class TupleMapTest {
    @Test
    public void testTupleMap() throws Exception {
        TupleMap<Integer, String> map = new TupleMap<Integer, String>();

        map.atKey(3).put("three");
        map.atKey(4).put("four");
        map.atKey(3, 2).put("nine");
        map.atKey(2, 3).put("eight");

        assertEquals("three", map.atKey(3).get());
        assertEquals("four", map.atKey(4).get());
        assertEquals("nine", map.atKey(3, 2).get());
        assertEquals("eight", map.atKey(2, 3).get());
        assertEquals(null, map.atKey(2, 4).get());
        assertEquals(null, map.atKey(1, 2, 3).get());
    }
}
