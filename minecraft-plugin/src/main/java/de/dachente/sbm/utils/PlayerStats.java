package de.dachente.sbm.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;

import de.dachente.sbm.main.Main;
import de.dachente.sbm.utils.enums.Language;


public class PlayerStats {
    public static void createPlayer(UUID uuid, String name) {
        String defaultLang = Language.EN.toString();

        String sql = "INSERT INTO players (uuid, playername, language, last_login, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

        try(Connection conn = Main.getDbManager().getConnection();
        var stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, uuid);
            stmt.setString(2, name);
            stmt.setString(3, defaultLang);

            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void loggedIn(UUID uuid) {
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

    public static CompletableFuture<Language> getLanguage(UUID uuid) {
        String sql = "SELECT language from players where uuid = ?";
        CompletableFuture<Language> future = new CompletableFuture<>(); 
        Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), () -> {
            try(Connection conn = Main.getDbManager().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setObject(1, uuid);

                try(ResultSet rs = stmt.executeQuery()) {
                    if(!rs.next()) future.complete(null) ;
                    future.complete(Language.valueOf(rs.getString("language")));
                }
                stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
            }
        });
        return future;
    }

    public static void updateLang(UUID uuid, Language lang) {
        String sql = "UPDATE players SET language = ? where uuid = ?;";
        Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), () -> {
            try(Connection conn = Main.getDbManager().getConnection();
            var stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, lang.toString());
                stmt.setObject(2, uuid);
                
                stmt.executeUpdate();
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
    
    public static CompletableFuture<Boolean> containsPlayer(UUID uuid) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        String sql = "SELECT 1 FROM players WHERE uuid = ? LIMIT 1;";
        Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), () -> {
            try (Connection conn = Main.getDbManager().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setObject(1, uuid);

                try (ResultSet rs = stmt.executeQuery()) {
                        future.complete(rs.next());
                }
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();;
                future.complete(false);
            }
        });
        return future;
    }
}
