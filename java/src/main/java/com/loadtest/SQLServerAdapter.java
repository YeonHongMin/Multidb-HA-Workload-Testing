package com.loadtest;

import java.sql.*;

/**
 * SQL Server JDBC 어댑터 (HikariCP 기반)
 */
public class SQLServerAdapter extends AbstractDatabaseAdapter {

    @Override
    public String buildJdbcUrl(DatabaseConfig config) {
        return String.format("jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false;trustServerCertificate=true",
                config.getHost(),
                config.getDefaultPort(),
                config.getDatabase());
    }

    @Override
    public String getDriverClassName() {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }

    @Override
    public long executeInsert(Connection conn, String threadId, String randomData) throws SQLException {
        String sql = """
            INSERT INTO load_test (thread_id, value_col, random_data, created_at)
            VALUES (?, ?, ?, GETDATE())
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
            VALUES (?, ?, ?, GETDATE())
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
        String sql = "UPDATE load_test SET value_col = ?, updated_at = GETDATE() WHERE id = ?";
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
        String sql = "SELECT ISNULL(MAX(id), 0) FROM load_test";
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
            -- SQL Server DDL
            IF OBJECT_ID('load_test', 'U') IS NOT NULL DROP TABLE load_test;
            CREATE TABLE load_test (
                id BIGINT IDENTITY(1,1) PRIMARY KEY,
                thread_id NVARCHAR(50) NOT NULL,
                value_col NVARCHAR(200),
                random_data NVARCHAR(1000),
                status NVARCHAR(20) DEFAULT 'ACTIVE',
                created_at DATETIME2 DEFAULT GETDATE(),
                updated_at DATETIME2 DEFAULT GETDATE()
            );
            CREATE INDEX idx_load_test_thread ON load_test(thread_id, created_at);
            """;
    }

    @Override
    public void setupSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 기존 테이블 삭제
            stmt.execute("IF OBJECT_ID('load_test', 'U') IS NOT NULL DROP TABLE load_test");

            // 테이블 생성
            stmt.execute("""
                CREATE TABLE load_test (
                    id BIGINT IDENTITY(1,1) PRIMARY KEY,
                    thread_id NVARCHAR(50) NOT NULL,
                    value_col NVARCHAR(200),
                    random_data NVARCHAR(1000),
                    status NVARCHAR(20) DEFAULT 'ACTIVE',
                    created_at DATETIME2 DEFAULT GETDATE(),
                    updated_at DATETIME2 DEFAULT GETDATE()
                )
                """);

            // 인덱스 생성
            stmt.execute("CREATE INDEX idx_load_test_thread ON load_test(thread_id, created_at)");

            conn.commit();
            logger.info("SQL Server schema created successfully");
        }
    }
}
