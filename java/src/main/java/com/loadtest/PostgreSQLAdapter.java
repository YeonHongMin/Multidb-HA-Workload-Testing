package com.loadtest;

import java.sql.*;

/**
 * PostgreSQL JDBC 어댑터 (HikariCP 기반)
 */
public class PostgreSQLAdapter extends AbstractDatabaseAdapter {

    @Override
    public String buildJdbcUrl(DatabaseConfig config) {
        return String.format("jdbc:postgresql://%s:%d/%s",
                config.getHost(),
                config.getDefaultPort(),
                config.getDatabase());
    }

    @Override
    public String getDriverClassName() {
        return "org.postgresql.Driver";
    }

    @Override
    public long executeInsert(Connection conn, String threadId, String randomData) throws SQLException {
        String sql = """
            INSERT INTO load_test (thread_id, value_col, random_data, created_at)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP) RETURNING id
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, threadId);
            ps.setString(2, "TEST_" + threadId);
            ps.setString(3, randomData);
            try (ResultSet rs = ps.executeQuery()) {
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
            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
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
        String sql = "UPDATE load_test SET value_col = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
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
            -- PostgreSQL DDL
            CREATE TABLE load_test (
                id BIGSERIAL PRIMARY KEY,
                thread_id VARCHAR(50) NOT NULL,
                value_col VARCHAR(200),
                random_data VARCHAR(1000),
                status VARCHAR(20) DEFAULT 'ACTIVE',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) PARTITION BY HASH (id);

            -- Create 16 partitions
            CREATE TABLE load_test_p00 PARTITION OF load_test FOR VALUES WITH (MODULUS 16, REMAINDER 0);
            CREATE TABLE load_test_p01 PARTITION OF load_test FOR VALUES WITH (MODULUS 16, REMAINDER 1);
            CREATE TABLE load_test_p02 PARTITION OF load_test FOR VALUES WITH (MODULUS 16, REMAINDER 2);
            CREATE TABLE load_test_p03 PARTITION OF load_test FOR VALUES WITH (MODULUS 16, REMAINDER 3);
            CREATE TABLE load_test_p04 PARTITION OF load_test FOR VALUES WITH (MODULUS 16, REMAINDER 4);
            CREATE TABLE load_test_p05 PARTITION OF load_test FOR VALUES WITH (MODULUS 16, REMAINDER 5);
            CREATE TABLE load_test_p06 PARTITION OF load_test FOR VALUES WITH (MODULUS 16, REMAINDER 6);
            CREATE TABLE load_test_p07 PARTITION OF load_test FOR VALUES WITH (MODULUS 16, REMAINDER 7);
            CREATE TABLE load_test_p08 PARTITION OF load_test FOR VALUES WITH (MODULUS 16, REMAINDER 8);
            CREATE TABLE load_test_p09 PARTITION OF load_test FOR VALUES WITH (MODULUS 16, REMAINDER 9);
            CREATE TABLE load_test_p10 PARTITION OF load_test FOR VALUES WITH (MODULUS 16, REMAINDER 10);
            CREATE TABLE load_test_p11 PARTITION OF load_test FOR VALUES WITH (MODULUS 16, REMAINDER 11);
            CREATE TABLE load_test_p12 PARTITION OF load_test FOR VALUES WITH (MODULUS 16, REMAINDER 12);
            CREATE TABLE load_test_p13 PARTITION OF load_test FOR VALUES WITH (MODULUS 16, REMAINDER 13);
            CREATE TABLE load_test_p14 PARTITION OF load_test FOR VALUES WITH (MODULUS 16, REMAINDER 14);
            CREATE TABLE load_test_p15 PARTITION OF load_test FOR VALUES WITH (MODULUS 16, REMAINDER 15);

            CREATE INDEX idx_load_test_thread ON load_test(thread_id, created_at);
            """;
    }

    @Override
    public void setupSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 기존 테이블 삭제
            stmt.execute("DROP TABLE IF EXISTS load_test CASCADE");

            // 파티션 테이블 생성
            stmt.execute("""
                CREATE TABLE load_test (
                    id BIGSERIAL PRIMARY KEY,
                    thread_id VARCHAR(50) NOT NULL,
                    value_col VARCHAR(200),
                    random_data VARCHAR(1000),
                    status VARCHAR(20) DEFAULT 'ACTIVE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) PARTITION BY HASH (id)
                """);

            // 16개 파티션 생성
            for (int i = 0; i < 16; i++) {
                stmt.execute(String.format(
                    "CREATE TABLE load_test_p%02d PARTITION OF load_test FOR VALUES WITH (MODULUS 16, REMAINDER %d)",
                    i, i));
            }

            // 인덱스 생성
            stmt.execute("CREATE INDEX idx_load_test_thread ON load_test(thread_id, created_at)");

            conn.commit();
            logger.info("PostgreSQL schema created successfully");
        }
    }
}
