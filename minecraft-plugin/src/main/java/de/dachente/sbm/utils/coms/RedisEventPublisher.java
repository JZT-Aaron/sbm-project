package de.dachente.sbm.utils.coms;

import com.google.gson.Gson;

import de.dachente.sbm.main.Main;
import redis.clients.jedis.Jedis;

public class RedisEventPublisher {
    private final Jedis jedis;
    private final Gson gson;

    public RedisEventPublisher(Jedis jedis) {
        this.jedis = jedis;
        this.gson = new Gson();
    }

    public void publishEvent(String channel, Object eventData) {
        try {
            String json = gson.toJson(eventData);
            jedis.publish(channel, json);
        } catch (Exception e) {
            Main.getPlugin().getLogger().warning("Error publishing event to Redis: " + e.getMessage());
        }
    }
}
