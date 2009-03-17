package org.atomserver.util;

import static junit.framework.Assert.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CollectionMapTest {

    static class Foo {
        String name;
        String bar;

        Foo(String name, String bar) {
            this.name = name;
            this.bar = bar;
        }
    }

    @Test
    public void testCollectionMap() throws Exception {
        List<Foo> list = new ArrayList<Foo>(
                Arrays.asList(new Foo("a", "A"), new Foo("b", "B"), new Foo("c", "C")));

        Map<String, Foo> map = new CollectionMap<String, Foo>(list) {
            protected String extractKey(Foo foo) {
                return foo.name;
            }
        };

        assertEquals("A", map.get("a").bar);
        assertEquals("B", map.get("b").bar);
        assertEquals("C", map.get("c").bar);
        assertEquals(null, map.get("z"));

        Foo d = new Foo("d", "D");
        list.add(d);

        assertEquals("D", map.get("d").bar);

        list.remove(d);

        assertEquals(null, map.get("d"));

        try {
            map.put("d", d);
            fail();
        } catch (UnsupportedOperationException e) {
            // expected this
        }

        try {
            map.remove("c");
            fail();
        } catch (UnsupportedOperationException e) {
            // expected this
        }
    }
}
