package org.atomserver.core.redis;

import org.jredis.JRedis;
import org.jredis.connector.ConnectionSpec;
import org.jredis.ri.alphazero.JRedisClient;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.InetAddress;

public class RedisUtilsTest {

    @AfterClass
    @BeforeClass
    public static void clearLocalRedis() throws Exception {
        new JRedisClient().flushall();
    }

    @Test
    public void testUrlValidation() throws Exception {
        // check these valid redis urls get accurately parsed into their components
        validateRedisUrl("redis://localhost:2345/0", "localhost", 2345, 0);
        validateRedisUrl("redis://localhost:2345", "localhost", 2345, 0);
        validateRedisUrl("redis://localhost/0", "localhost", 6379, 0);
        validateRedisUrl("redis://localhost", "localhost", 6379, 0);
        validateRedisUrl("redis://localhost:2345/9", "localhost", 2345, 9);
        validateRedisUrl("redis://localhost/11", "localhost", 6379, 11);

        // protocol invalid
        assertInvalidRedisUrl("http://localhost");
        assertInvalidRedisUrl("http://localhost:8079");
        assertInvalidRedisUrl("http://localhost:8079/0");

        // db number invalid
        assertInvalidRedisUrl("redis://localhost/16");
        assertInvalidRedisUrl("redis://localhost/database");
        assertInvalidRedisUrl("redis://localhost:8079/16");
        assertInvalidRedisUrl("redis://localhost:8079/database");

        // port invalid
        assertInvalidRedisUrl("redis://localhost:porto");
        assertInvalidRedisUrl("redis://localhost:porto/0");

        // host invalid
        assertInvalidRedisUrl("redis://b@dhost");
        assertInvalidRedisUrl("redis://b@dhost:8079");
        assertInvalidRedisUrl("redis://b@dhost:8079/0");
    }

    @Test
    public void testUrlConnection() throws Exception {
        JRedis[] clients = {
                RedisUtils.connect("redis://localhost"),
                RedisUtils.connect("redis://localhost:6379/4"),
                RedisUtils.connect("redis://localhost/13")
        };

        clients[0].set("key0", "value0");
        clients[1].set("key1", "value1");
        Assert.assertNull(clients[0].get("key1"));
        Assert.assertNull(clients[1].get("key0"));
        Assert.assertNull(clients[2].get("key1"));
        Assert.assertNull(clients[2].get("key0"));
        Assert.assertArrayEquals("value0".getBytes(), clients[0].get("key0"));
        Assert.assertArrayEquals("value1".getBytes(), clients[1].get("key1"));
    }

    private void assertInvalidRedisUrl(String url) {
        try {
            RedisUtils.parseRedisUrl(url);
            Assert.fail("expected an exception");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().startsWith(url + " is not a valid redis URL"));
        }
    }

    private void validateRedisUrl(String url, String hostname, int port, int db) throws Exception {
        ConnectionSpec spec = RedisUtils.parseRedisUrl(url);
        Assert.assertEquals(InetAddress.getByName(hostname), spec.getAddress());
        Assert.assertEquals(port, spec.getPort());
        Assert.assertEquals(db, spec.getDatabase());
    }


}
