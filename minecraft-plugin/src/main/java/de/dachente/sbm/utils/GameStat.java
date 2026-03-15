package de.dachente.sbm.utils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.reflect.TypeToken;

import de.dachente.sbm.utils.enums.GameMap;
import de.dachente.sbm.utils.enums.GameState;
import de.dachente.sbm.utils.enums.Language;
import de.dachente.sbm.utils.enums.Status;
import de.dachente.sbm.utils.enums.Team;

public enum GameStat {
    STATE("state", GameState.class, GameState.CLOSED),
    TEAM_PLAYERS("team-players", new TypeToken<Map<String, Team>>(){}.getType(), new HashMap<>()),
    LIVING_PLAYERS("living-players", new TypeToken<Map<String, Team>>(){}.getType(), new HashMap<>()),
    TEAM_HEARTS("team-hearts", new TypeToken<Map<Team, Integer>>(){}.getType(), new HashMap<>()),
    GAME_END_TIMESTAMP("game-end-timestamp", Long.class, null),
    PLAYER_STATUS("player-status", new TypeToken<Map<String, Status>>(){}.getType(), new HashMap<>()),
    PLAYER_LANGUAGE("player-language", new TypeToken<Map<String, Language>>(){}.getType(), new HashMap<>()),
    LOADED_MAP("loaded-map", GameMap.class, GameMap.GAME);
    

    private final String redisKey;
    private final Type type;
    private final Object defaultValue;

    GameStat(String redisKey, Type type, Object defaultValue) {
        this.redisKey = redisKey;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public String getRedisKey() {
        return redisKey;
    }

    public Type getType() {
        return type;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }
}
