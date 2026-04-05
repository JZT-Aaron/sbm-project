package de.dachente.sbm.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.EnumMap;

import java.util.Map;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.utils.coms.RedisManager;

public class GameStats {
    private static final RedisManager redisManager = Main.getRedisManager();
    private static Map<GameStat, Object> values = new EnumMap<>(GameStat.class);

    @SuppressWarnings("unchecked")
    public static <T> T get(GameStat stat) {
        return (T) values.computeIfAbsent(stat, s -> s.getDefaultValue());
    }

    public static void set(GameStat stat, Object value) {
        if (!isAssignableFrom(stat.getType(), value)) 
            throw new IllegalArgumentException("Wrong Type used for " + stat.name());
        
        values.put(stat, value);
        redisManager.updateStat(stat.getRedisKey(), value);
    }

    private static boolean isAssignableFrom(Type type, Object value) {
        if(value == null) return true;

        Class<?> clazz = null;

        if (type instanceof Class<?>) clazz = (Class<?>) type;
         else if (type instanceof ParameterizedType) clazz = (Class<?>) ((ParameterizedType) type).getRawType();

        if(clazz == null) throw new IllegalArgumentException("Assignable Check could not make out Type");
        
        if (clazz.isPrimitive()) {
            if (clazz == int.class) clazz = Integer.class;
            if (clazz == long.class) clazz = Long.class;
            if (clazz == double.class) clazz = Double.class;
            if (clazz == boolean.class) clazz = Boolean.class;
        }
        
        return clazz.isInstance(value);
    }

    public static void init() {
        Map<String, String> remoteData = redisManager.getAllStats();
        for(GameStat stat : GameStat.values()) {
            String redisKey = stat.getRedisKey();
            Object data;


            if(remoteData.containsKey(redisKey)) {
                data = redisManager.parse(remoteData.get(redisKey), stat.getType());
                values.put(stat, data); 
            } else {
                data = stat.getDefaultValue();
                set(stat, stat.getDefaultValue());
            }
            Main.getPlugin().getLogger().info(stat.getRedisKey() + " has bin set: " + data);
            
        }
        Main.getPlugin().getLogger().info("Gamestats succesfully initialized");
    }
}
