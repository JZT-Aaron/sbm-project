package de.dachente.sbm.utils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.google.gson.reflect.TypeToken;

import de.dachente.sbm.utils.enums.GameMap;
import de.dachente.sbm.utils.enums.GameState;
import de.dachente.sbm.utils.enums.Language;
import de.dachente.sbm.utils.enums.Team;

public enum GameStat {
    STATE("state", GameState.class, () -> GameState.CLOSED),
    TEAM_PLAYERS("team-players", new TypeToken<Map<String, Team>>(){}.getType(), HashMap::new),
    LIVING_PLAYERS("living-players", new TypeToken<Map<String, Team>>(){}.getType(), HashMap::new),
    TEAM_HEARTS("team-hearts", new TypeToken<Map<Team, Integer>>(){}.getType(), HashMap::new),
    GAME_END_TIMESTAMP("game-end-timestamp", Long.class, () ->null),
    PLAYER_LANGUAGE("player-language", new TypeToken<Map<String, Language>>(){}.getType(), HashMap::new),
    LOADED_MAP("loaded-map", GameMap.class, () -> GameMap.GAME),
    LEFT_TEAM_PLAYERS("left-team-players", new TypeToken<Map<String, Game.HandoverContext>>(){}.getType(), HashMap::new),
    NEXT_RESPAWN_POINT("next-respawn-point", new TypeToken<Map<Team, Integer>>(){}.getType(), HashMap::new),
    SPECTATOR_PLAYERS("spectator-players", new TypeToken<Map<String, String>>(){}.getType(), HashMap::new);
    

    private final String redisKey;
    private final Type type;
    private final Supplier<Object> defaultSupplier;

    GameStat(String redisKey, Type type, Supplier<Object> defaultSupplier) {
        this.redisKey = redisKey;
        this.type = type;
        this.defaultSupplier = defaultSupplier;
    }

    public String getRedisKey() {
        return redisKey;
    }

    public Type getType() {
        return type;
    }

    public Object getDefaultValue() {
        return defaultSupplier.get();
    }
}
