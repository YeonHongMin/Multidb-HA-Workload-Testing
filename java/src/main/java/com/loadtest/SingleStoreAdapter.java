package com.loadtest;

import com.zaxxer.hikari.HikariConfig;

import java.sql.*;

/**
 * SingleStore JDBC 어댑터 (HikariCP 기반)
 * SingleStore는 MySQL 호환 프로토콜을 사용하므로 MySQL과 유사한 구조
 */
public class SingleStoreAdapter extends AbstractDatabaseAdapter {

    private static final int SINGLESTORE_MAX_POOL_SIZE = 32;

    @Override
    public void createConnectionPool(DatabaseConfig config) {
        // SingleStore 커넥션 풀 크기 제한
        int effectiveMin = Math.min(config.getMinPoolSize(), SINGLESTORE_MAX_POOL_SIZE);
        int effectiveMax = Math.min(config.getMaxPoolSize(), SINGLESTORE_MAX_POOL_SIZE);

        if (config.getMinPoolSize() > SINGLESTORE_MAX_POOL_SIZE || config.getMaxPoolSize() > SINGLESTORE_MAX_POOL_SIZE) {
            logger.warn("[SingleStore] Pool size limited to {} (requested: min={}, max={}). " +
                    "See SINGLESTORE_MAX_POOL_SIZE for details.",
                    SINGLESTORE_MAX_POOL_SIZE, config.getMinPoolSize(), config.getMaxPoolSize());
        }

        DatabaseConfig adjustedConfig = DatabaseConfig.builder()
                .dbType(config.getDbType())
                .host(config.getHost())
                .port(config.getPort())
                .database(config.getDatabase())
                .user(config.getUser())
                .password(config.getPassword())
                .minPoolSize(effectiveMin)
                .maxPoolSize(effectiveMax)
                .maxLifetimeSeconds(config.getMaxLifetimeSeconds())
                .leakDetectionThresholdSeconds(config.getLeakDetectionThresholdSeconds())
                .idleCheckIntervalSeconds(config.getIdleCheckIntervalSeconds())
                .idleTimeoutSeconds(config.getIdleTimeoutSeconds())
                .keepaliveTimeSeconds(config.getKeepaliveTimeSeconds())
                .connectionTimeoutMs(config.getConnectionTimeoutMs())
                .validationTimeoutMs(config.getValidationTimeoutMs())
                .build();

        super.createConnectionPool(adjustedConfig);
    }

    @Override
    protected void configureDataSourceProperties(HikariConfig hikariConfig, DatabaseConfig config) {
        // SingleStore 특수 설정 (MySQL 호환)
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
    }

    @Override
    public String buildJdbcUrl(DatabaseConfig config) {
        return String.format("jdbc:singlestore://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                config.getHost(),
                config.getDefaultPort(),
                config.getDatabase());
    }

    @Override
    public String getDriverClassName() {
        return "com.singlestore.jdbc.Driver";
    }

    @Override
    public long executeInsert(Connection conn, String threadId, String randomData) throws SQLException {
        String sql = """
            INSERT INTO load_test (thread_id, value_col, random_data, created_at)
            VALUES (?, ?, ?, NOW())
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, threadId);
            ps.setString(2, "TEST_" + threadId);
            ps.setString(3, randomData);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1;
    }

    @Override
    public int executeBatchInsert(Connection conn, String threadId, int batchSize) throws SQLException {
        String sql = """
            INSERT INTO load_test (thread_id, value_col, random_data, created_at)
            VALUES (?, ?, ?, NOW())
            """;
        String randomData = generateRandomData(500);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < batchSize; i++) {
                ps.setString(1, threadId);
                ps.setString(2, "TEST_" + threadId);
                ps.setString(3, randomData);
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return batchSize;
    }

    @Override
    public Object[] executeSelect(Connection conn, long recordId) throws SQLException {
        String sql = "SELECT id, thread_id, value_col FROM load_test WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, recordId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Object[]{rs.getLong(1), rs.getString(2), rs.getString(3)};
                }
            }
        }
        return null;
    }

    @Override
    public Object[] executeRandomSelect(Connection conn, long maxId) throws SQLException {
        if (maxId <= 0) return null;
        return executeSelect(conn, getRandomId(maxId));
    }

    @Override
    public boolean executeUpdate(Connection conn, long recordId) throws SQLException {
        String sql = "UPDATE load_test SET value_col = ?, updated_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "UPDATED_" + recordId);
            ps.setLong(2, recordId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean executeDelete(Connection conn, long recordId) throws SQLException {
        String sql = "DELETE FROM load_test WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, recordId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public long getMaxId(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(id), 0) FROM load_test";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0;
    }

    @Override
    public String getDDL() {
        return """
            -- SingleStore DDL
            CREATE TABLE load_test (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                thread_id VARCHAR(50) NOT NULL,
                value_col VARCHAR(200),
                random_data VARCHAR(1000),
                status VARCHAR(20) DEFAULT 'ACTIVE',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                KEY idx_load_test_thread (thread_id, created_at)
            );
            """;
    }

    @Override
    public void setupSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 테이블 존재 여부 확인
            boolean tableExists = false;
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'load_test'")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    tableExists = true;
                }
            }

            if (tableExists) {
                logger.info("SingleStore schema already exists - reusing existing schema");
                logger.info("  (DROP TABLE load_test to recreate, or use --truncate to clear data only)");
                return;
            }

            // 테이블 생성
            stmt.execute("""
                CREATE TABLE load_test (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    thread_id VARCHAR(50) NOT NULL,
                    value_col VARCHAR(200),
                    random_data VARCHAR(1000),
                    status VARCHAR(20) DEFAULT 'ACTIVE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    KEY idx_load_test_thread (thread_id, created_at)
                )
                """);

            conn.commit();
            logger.info("SingleStore schema created successfully");
        }
    }

    @Override
    public void truncateTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // TRUNCATE TABLE automatically resets AUTO_INCREMENT in SingleStore
            stmt.execute("TRUNCATE TABLE load_test");
            conn.commit();
            logger.info("Table load_test truncated and AUTO_INCREMENT reset to 1");
        }
    }
}
