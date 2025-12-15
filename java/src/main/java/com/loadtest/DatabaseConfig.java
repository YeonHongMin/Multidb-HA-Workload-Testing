package com.loadtest;

/**
 * 데이터베이스 연결 설정
 */
public class DatabaseConfig {
    private String dbType;
    private String host;
    private String user;
    private String password;
    private String database;
    private String sid;
    private int port;
    private int minPoolSize = 100;
    private int maxPoolSize = 200;
    private int maxLifetimeSeconds = 1800;  // 30분
    private int leakDetectionThresholdSeconds = 60;
    private int idleCheckIntervalSeconds = 30;
    private int idleTimeoutSeconds = 30;  // 유휴 커넥션 제거 시간
    private int keepaliveTimeSeconds = 30;  // 유휴 커넥션 검증 주기 (HikariCP 최소값: 30초)
    private int connectionTimeoutMs = 30000;
    private int validationTimeoutMs = 5000;

    public DatabaseConfig() {}

    public DatabaseConfig(String dbType, String host, String user, String password) {
        this.dbType = dbType;
        this.host = host;
        this.user = user;
        this.password = password;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final DatabaseConfig config = new DatabaseConfig();

        public Builder dbType(String dbType) {
            config.dbType = dbType;
            return this;
        }

        public Builder host(String host) {
            config.host = host;
            return this;
        }

        public Builder user(String user) {
            config.user = user;
            return this;
        }

        public Builder password(String password) {
            config.password = password;
            return this;
        }

        public Builder database(String database) {
            config.database = database;
            return this;
        }

        public Builder sid(String sid) {
            config.sid = sid;
            return this;
        }

        public Builder port(int port) {
            config.port = port;
            return this;
        }

        public Builder minPoolSize(int minPoolSize) {
            config.minPoolSize = minPoolSize;
            return this;
        }

        public Builder maxPoolSize(int maxPoolSize) {
            config.maxPoolSize = maxPoolSize;
            return this;
        }

        public Builder maxLifetimeSeconds(int maxLifetimeSeconds) {
            config.maxLifetimeSeconds = maxLifetimeSeconds;
            return this;
        }

        public Builder leakDetectionThresholdSeconds(int leakDetectionThresholdSeconds) {
            config.leakDetectionThresholdSeconds = leakDetectionThresholdSeconds;
            return this;
        }

        public Builder idleCheckIntervalSeconds(int idleCheckIntervalSeconds) {
            config.idleCheckIntervalSeconds = idleCheckIntervalSeconds;
            return this;
        }

        public Builder idleTimeoutSeconds(int idleTimeoutSeconds) {
            config.idleTimeoutSeconds = idleTimeoutSeconds;
            return this;
        }

        public Builder keepaliveTimeSeconds(int keepaliveTimeSeconds) {
            config.keepaliveTimeSeconds = keepaliveTimeSeconds;
            return this;
        }

        public Builder connectionTimeoutMs(int connectionTimeoutMs) {
            config.connectionTimeoutMs = connectionTimeoutMs;
            return this;
        }

        public Builder validationTimeoutMs(int validationTimeoutMs) {
            config.validationTimeoutMs = validationTimeoutMs;
            return this;
        }

        public DatabaseConfig build() {
            return config;
        }
    }

    // Getters and Setters
    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getMaxLifetimeSeconds() {
        return maxLifetimeSeconds;
    }

    public void setMaxLifetimeSeconds(int maxLifetimeSeconds) {
        this.maxLifetimeSeconds = maxLifetimeSeconds;
    }

    public int getLeakDetectionThresholdSeconds() {
        return leakDetectionThresholdSeconds;
    }

    public void setLeakDetectionThresholdSeconds(int leakDetectionThresholdSeconds) {
        this.leakDetectionThresholdSeconds = leakDetectionThresholdSeconds;
    }

    public int getIdleCheckIntervalSeconds() {
        return idleCheckIntervalSeconds;
    }

    public void setIdleCheckIntervalSeconds(int idleCheckIntervalSeconds) {
        this.idleCheckIntervalSeconds = idleCheckIntervalSeconds;
    }

    public int getIdleTimeoutSeconds() {
        return idleTimeoutSeconds;
    }

    public void setIdleTimeoutSeconds(int idleTimeoutSeconds) {
        this.idleTimeoutSeconds = idleTimeoutSeconds;
    }

    public int getKeepaliveTimeSeconds() {
        return keepaliveTimeSeconds;
    }

    public void setKeepaliveTimeSeconds(int keepaliveTimeSeconds) {
        this.keepaliveTimeSeconds = keepaliveTimeSeconds;
    }

    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(int connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public int getValidationTimeoutMs() {
        return validationTimeoutMs;
    }

    public void setValidationTimeoutMs(int validationTimeoutMs) {
        this.validationTimeoutMs = validationTimeoutMs;
    }

    public int getDefaultPort() {
        if (port > 0) return port;
        return switch (dbType.toLowerCase()) {
            case "oracle" -> 1521;
            case "postgresql", "postgres", "pg" -> 5432;
            case "mysql" -> 3306;
            case "sqlserver", "mssql" -> 1433;
            case "tibero" -> 8629;
            case "db2" -> 50000;
            default -> 0;
        };
    }
}
