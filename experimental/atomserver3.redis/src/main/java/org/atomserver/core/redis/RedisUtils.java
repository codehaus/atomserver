package org.atomserver.core.redis;

import org.apache.commons.lang.StringUtils;
import org.jredis.JRedis;
import org.jredis.connector.ConnectionSpec;
import org.jredis.ri.alphazero.JRedisClient;
import org.jredis.ri.alphazero.connection.DefaultConnectionSpec;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RedisUtils {
    public static final int DEFAULT_REDIS_DB = 0;
    public static final int DEFAULT_REDIS_PORT = 6379;

    private static final Pattern REDIS_URL_PATTERN =
            Pattern.compile("redis://([a-zA-Z0-9\\-\\_\\.]+)(?:\\:(\\d+))?(?:/(\\d|(?:1[0-5])))?");

    public static ConnectionSpec parseRedisUrl(String url) {
        final Matcher matcher = REDIS_URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    String.format("%s is not a valid redis URL\n" +
                            "********************************************************************************\n" +
                            "A redis URL should be of the form:\n" +
                            "  redis://<host>[:port][/db]\n" +
                            "    host - the hostname or IP of the redis server\n" +
                            "    port - the port on which redis is running (defaults to 6379)\n" +
                            "    db   - the DB number to connect - must be in range [0,16) (defaults to 0)\n" +
                            "  examples:\n" +
                            "    redis://localhost (default port and db)\n" +
                            "    redis://redis.server:8079 (port 8079, defaut db)\n" +
                            "    redis://redis.server:8079/11 (port 8079, db 11)\n" +
                            "    redis://redis.server/9 (default port, db 9)\n" +
                            "********************************************************************************",
                            url));
        }
        String host = matcher.group(1);
        String port = matcher.group(2);
        String db = matcher.group(3);

        return DefaultConnectionSpec.newSpec(
                host,
                port == null ? DEFAULT_REDIS_PORT : Integer.parseInt(port),
                StringUtils.isEmpty(db) ? DEFAULT_REDIS_DB : Integer.parseInt(db),
                null);
    }

    public static JRedis connect(String url) {
        return connect(url, null);
    }

    public static JRedis connect(String url, String password) {
        final ConnectionSpec spec = parseRedisUrl(url);
        if (password != null) {
            spec.setCredentials(password.getBytes());
        }
        return new JRedisClient(spec);
    }


}
