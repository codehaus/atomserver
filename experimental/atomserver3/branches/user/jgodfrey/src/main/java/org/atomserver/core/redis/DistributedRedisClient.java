package org.atomserver.core.redis;

import org.atomserver.sharding.Distribution;
import org.atomserver.sharding.Selector;
import org.jredis.JRedis;

import java.util.HashMap;
import java.util.Map;

public class DistributedRedisClient {
    Distribution.CompiledDistribution compiledDistribution;
    private final Map<String, JRedis> map = new HashMap<String, JRedis>();

    public DistributedRedisClient(Distribution distribution) {
        for (Selector selector : distribution.getSelectors()) {
            for (String url : selector.getUrls()) {
                if (!map.containsKey(url)) {
                    map.put(url, RedisUtils.connect(url)); // TODO: password
                }
            }
        }
        this.compiledDistribution = distribution.compile();
    }

    public JRedis at(String key) {
        return map.get(compiledDistribution.map(key));
    }
}
