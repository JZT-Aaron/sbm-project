package de.dachente.sbm.utils.coms;

import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import de.dachente.sbm.main.Main;

public class DatabaseManager {
    private HikariDataSource dataSource;

    public DatabaseManager(String host, int port, String database, String user, String password) {
        HikariConfig config = new HikariConfig();

        config.setDriverClassName("org.postgresql.Driver");

        config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
        config.setUsername(user);
        config.setPassword(password);

        config.setIdleTimeout(30000);   
        config.setMaxLifetime(60000);    
        config.setConnectionTimeout(5000); 
        config.setMinimumIdle(2);        
        config.setMaximumPoolSize(10);

        this.dataSource = new HikariDataSource(config);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if(dataSource == null) return; 
        dataSource.close();
        Main.getPlugin().getLogger().info("DB Connection Closed.");
    }
}
