package com.example.DACN.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@Configuration
@Slf4j
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Bean
    @Primary
    public DataSource dataSource() {
        // Create database if not exists BEFORE creating datasource
        createDatabaseIfNotExists();

        // Create HikariCP datasource
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(datasourceUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);

        // Connection pool settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        return new HikariDataSource(config);
    }

    private void createDatabaseIfNotExists() {
        try {
            String databaseName = extractDatabaseName(datasourceUrl);
            String baseUrl = getBaseUrl(datasourceUrl);

            log.info("🔍 Checking if database '{}' exists...", databaseName);

            try (Connection conn = DriverManager.getConnection(baseUrl, username, password);
                    Statement stmt = conn.createStatement()) {

                String createDbSql = String.format(
                        "CREATE DATABASE IF NOT EXISTS `%s` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
                        databaseName);

                stmt.executeUpdate(createDbSql);
                log.info("✅ Database '{}' is ready!", databaseName);

            } catch (Exception e) {
                log.error("❌ Failed to create database: {}", e.getMessage());
                log.error("💡 Please check MySQL is running and credentials are correct");
                throw new RuntimeException("Database initialization failed", e);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private String extractDatabaseName(String url) {
        String dbPart = url.substring(url.lastIndexOf("/") + 1);
        if (dbPart.contains("?")) {
            dbPart = dbPart.substring(0, dbPart.indexOf("?"));
        }
        return dbPart;
    }

    private String getBaseUrl(String url) {
        String baseUrl = url.substring(0, url.lastIndexOf("/"));
        if (url.contains("?")) {
            String params = url.substring(url.indexOf("?"));
            baseUrl = url.substring(0, url.indexOf("?"));
            baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf("/"));
            baseUrl += params;
        }
        return baseUrl;
    }
}
