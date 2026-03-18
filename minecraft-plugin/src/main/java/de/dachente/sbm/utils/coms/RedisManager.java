package de.dachente.sbm.utils.coms;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gson.Gson;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisManager {
    private static final int THRESHOLD_BYTES = 1024;
    private JedisPool pool;
    private Gson gson = new Gson();

    private static final String REDIS_KEY_HASH = "game:stats";
    private static final String REDIS_CHANNEL = "GAMESTATE_UPDATE";

    public RedisManager(String host, int port, String password) {
        this.pool = new JedisPool(new JedisPoolConfig(), host, port, 2000, password);
    }

    public void updateStat(String field, Object value) {
        String stringValue = (value instanceof String) ? (String) value : gson.toJson(value);
        boolean isHeavy = stringValue.length() > THRESHOLD_BYTES;

        CompletableFuture.runAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.hset(REDIS_KEY_HASH, field, stringValue);

                Map<String, Object> packet = new HashMap<>();
                packet.put("f", field);
                packet.put("h", isHeavy);
                packet.put("v", isHeavy ? null : value);

                jedis.publish(REDIS_CHANNEL, gson.toJson(packet));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }); 
    }

    public Map<String, String> getAllStats() {
        try (Jedis jedis = pool.getResource()) {
            return jedis.hgetAll(REDIS_KEY_HASH);
        }
    }

    public <T> T parse(String raw, Type type) {
        if(raw == null || raw.equalsIgnoreCase("null")) return null;
        if(type == String.class) return (T) raw;
        if(type == Integer.class || type == int.class) return (T) Integer.valueOf(raw);
        if(type == Long.class || type == long.class) return (T) Long.valueOf(raw);
        return gson.fromJson(raw, type);
    }

    public void close() {
        if(pool != null) pool.close();
    }

}