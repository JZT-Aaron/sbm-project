package de.dachente.sbm.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.utils.enums.Language;
import de.dachente.sbm.utils.enums.PlayerStat;


public class PlayerStats {
    private static List<UUID> snowPlayers = new ArrayList<>();


    public static void setupDatebase() {
        String sql = "CREATE TABLE IF NOT EXISTS players (" +
                 "uuid UUID PRIMARY KEY, " +
                 "playername VARCHAR(16) NOT NULL, " +
                 "language VARCHAR(10), " +
                 "playerstatus VARCHAR(16), " +
                 "snowing BOOLEAN DEFAULT FALSE, " +
                 "last_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                 "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                 ");";

        try (Connection conn = Main.getDbManager().getConnection();
            Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                stmt.close();
                
            } catch (SQLException e) {
                Main.getPlugin().getLogger().warning("Error at Creating Table: " + e.getMessage());
            }
    }

    public static void createPlayerSync(UUID uuid, String name) {
        String defaultLang = Language.EN.toString();

        String sql = "INSERT INTO players (uuid, playername, language, last_login, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

        try(Connection conn = Main.getDbManager().getConnection();
        var stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, uuid);
            stmt.setString(2, name);
            stmt.setString(3, defaultLang);

            stmt.executeUpdate();
            stmt.close();
            Main.getPlugin().getLogger().info("PlayerCreated in DB: " + uuid + " | " + name + " | " + defaultLang);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean containsPlayerSync(UUID uuid) {
        String sql = "SELECT 1 FROM players WHERE uuid = ? LIMIT 1;";
        boolean containsPlayer = false;

        try (Connection conn = Main.getDbManager().getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, uuid);

            try (ResultSet rs = stmt.executeQuery()) {
                containsPlayer = rs.next();
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();;
            return false;
        }
        return containsPlayer;
    }

    public static void loggedInSync(UUID uuid) {
        updateShowListFor(uuid);
        String sql = "UPDATE players SET last_login = CURRENT_TIMESTAMP where uuid = ?;";
        Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), () -> {
            try(Connection conn = Main.getDbManager().getConnection();
            var stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, uuid);

                stmt.executeUpdate();
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public static CompletableFuture<Language> getString(PlayerStat playerStat, UUID uuid) {
         CompletableFuture<Language> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), () -> {
            future.complete(getLanguageSync(uuid));
        });
        return future;
    }

    public static String getStringSync(PlayerStat playerStat, UUID uuid) {
        String sql = "SELECT " + playerStat.id() + " from players where uuid = ?";
        String output = null;
        
        try(Connection conn = Main.getDbManager().getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setObject(1, uuid);
                
                
                try(ResultSet rs = stmt.executeQuery()) {
                    if(!rs.next()) output = null;
                    else output = rs.getString(playerStat.id());
                }
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        return output;
    }

    public static void updateString(PlayerStat playerStat, UUID uuid, String string) {
        Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), () -> updateStringSync(playerStat, uuid, string));
    }

    public static void updateBoolean(PlayerStat playerStat, UUID uuid, boolean booleanValue) {
        Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), () -> updateBooleanSync(playerStat, uuid, booleanValue));
    }

    public static void updateStringSync(PlayerStat playerStat, UUID uuid, String string) {
        String sql = "UPDATE players SET "+ playerStat.id() +" = ? where uuid = ?;";
        try(Connection conn = Main.getDbManager().getConnection();
            var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, string);
            stmt.setObject(2, uuid);
            
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateBooleanSync(PlayerStat playerStat, UUID uuid, boolean booleanV) {
        String sql = "UPDATE players SET "+ playerStat.id() +" = ? where uuid = ?;";
        try(Connection conn = Main.getDbManager().getConnection();
            var stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, booleanV);
            stmt.setObject(2, uuid);
            
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static CompletableFuture<Language> getLanguage(UUID uuid) {
        CompletableFuture<Language> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), () -> {
            future.complete(getLanguageSync(uuid));
        });
        return future;
    }

    public static Language getLanguageSync(UUID uuid) {
        return Language.valueOf(getStringSync(PlayerStat.LANGUAGE, uuid));
    }

    public static void updateLang(UUID uuid, Language lang) {
        Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), () -> updateStringSync(PlayerStat.LANGUAGE, uuid, lang.toString()));
    }

    public static void addSnowPlayer(UUID uuid) {
        snowPlayers.add(uuid);
        updateBoolean(PlayerStat.SNOWING, uuid, true);
    }

    public static void removeSnowPlayer(UUID uuid) {
        snowPlayers.remove(uuid);
        updateBoolean(PlayerStat.SNOWING, uuid, false);
    }

    public static void setOffline(UUID uuid) {
        snowPlayers.remove(uuid);
    }

    public static List<UUID> getSnowPlayers() {
        return snowPlayers;
    }

    public static void updateShowListFor(UUID uuid) {
        boolean isSnowing = getStringSync(PlayerStat.SNOWING, uuid) == "TRUE";
        if(isSnowing) snowPlayers.add(uuid);
        else snowPlayers.remove(uuid);
    }

    public static void initSnowList() {
        snowPlayers.clear();
        String sql = "SELECT uuid FROM players WHERE snowing = TRUE";
        try (Connection conn = Main.getDbManager().getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    if(Bukkit.getPlayer(uuid) == null) continue;
                    snowPlayers.add(uuid);
                }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
        
}
