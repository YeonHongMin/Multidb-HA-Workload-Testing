package com.loadtest;

import java.sql.*;

/**
 * IBM DB2 JDBC 어댑터 (HikariCP 기반)
 */
public class DB2Adapter extends AbstractDatabaseAdapter {

    @Override
    public String buildJdbcUrl(DatabaseConfig config) {
        return String.format("jdbc:db2://%s:%d/%s",
                config.getHost(),
                config.getDefaultPort(),
                config.getDatabase());
    }

    @Override
    public String getDriverClassName() {
        return "com.ibm.db2.jcc.DB2Driver";
    }

    @Override
    protected String getValidationQuery() {
        return "SELECT 1 FROM SYSIBM.SYSDUMMY1";
    }

    @Override
    public long executeInsert(Connection conn, String threadId, String randomData) throws SQLException {
        String sql = """
            INSERT INTO LOAD_TEST (ID, THREAD_ID, VALUE_COL, RANDOM_DATA, CREATED_AT)
            VALUES (NEXT VALUE FOR LOAD_TEST_SEQ, ?, ?, ?, CURRENT TIMESTAMP)
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, threadId);
            ps.setString(2, "TEST_" + threadId);
            ps.setString(3, randomData);
            ps.executeUpdate();
        }

        // 생성된 ID 조회
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT PREVIOUS VALUE FOR LOAD_TEST_SEQ FROM SYSIBM.SYSDUMMY1")) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return -1;
    }

    @Override
    public int executeBatchInsert(Connection conn, String threadId, int batchSize) throws SQLException {
        String sql = """
            INSERT INTO LOAD_TEST (ID, THREAD_ID, VALUE_COL, RANDOM_DATA, CREATED_AT)
            VALUES (NEXT VALUE FOR LOAD_TEST_SEQ, ?, ?, ?, CURRENT TIMESTAMP)
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
        String sql = "SELECT ID, THREAD_ID, VALUE_COL FROM LOAD_TEST WHERE ID = ?";
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
        String sql = "UPDATE LOAD_TEST SET VALUE_COL = ?, UPDATED_AT = CURRENT TIMESTAMP WHERE ID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "UPDATED_" + recordId);
            ps.setLong(2, recordId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean executeDelete(Connection conn, long recordId) throws SQLException {
        String sql = "DELETE FROM LOAD_TEST WHERE ID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, recordId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public long getMaxId(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(ID), 0) FROM LOAD_TEST";
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
            -- IBM DB2 DDL
            CREATE SEQUENCE LOAD_TEST_SEQ START WITH 1 INCREMENT BY 1 CACHE 1000 NO CYCLE ORDER;

            CREATE TABLE LOAD_TEST (
                ID           BIGINT          NOT NULL,
                THREAD_ID    VARCHAR(50)     NOT NULL,
                VALUE_COL    VARCHAR(200),
                RANDOM_DATA  VARCHAR(1000),
                STATUS       VARCHAR(20)     DEFAULT 'ACTIVE',
                CREATED_AT   TIMESTAMP       DEFAULT CURRENT TIMESTAMP,
                UPDATED_AT   TIMESTAMP       DEFAULT CURRENT TIMESTAMP,
                PRIMARY KEY (ID)
            );

            CREATE INDEX IDX_LOAD_TEST_THREAD ON LOAD_TEST(THREAD_ID, CREATED_AT);
            """;
    }

    @Override
    public void setupSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 테이블 존재 여부 확인
            boolean tableExists = false;
            boolean seqExists = false;

            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM SYSCAT.TABLES WHERE TABNAME = 'LOAD_TEST'")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    tableExists = true;
                }
            }

            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM SYSCAT.SEQUENCES WHERE SEQNAME = 'LOAD_TEST_SEQ'")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    seqExists = true;
                }
            }

            if (tableExists && seqExists) {
                logger.info("DB2 schema already exists - reusing existing schema");
                logger.info("  (DROP objects manually to recreate, or use --truncate to clear data only)");
                return;
            }

            // 기존 객체 삭제 (일부만 존재하는 경우)
            if (seqExists) {
                try {
                    stmt.execute("DROP SEQUENCE LOAD_TEST_SEQ");
                } catch (SQLException ignored) {}
            }
            if (tableExists) {
                try {
                    stmt.execute("DROP TABLE LOAD_TEST");
                } catch (SQLException ignored) {}
            }

            // 시퀀스 생성
            stmt.execute("CREATE SEQUENCE LOAD_TEST_SEQ START WITH 1 INCREMENT BY 1 CACHE 1000 NO CYCLE ORDER");

            // 테이블 생성
            stmt.execute("""
                CREATE TABLE LOAD_TEST (
                    ID BIGINT NOT NULL,
                    THREAD_ID VARCHAR(50) NOT NULL,
                    VALUE_COL VARCHAR(200),
                    RANDOM_DATA VARCHAR(1000),
                    STATUS VARCHAR(20) DEFAULT 'ACTIVE',
                    CREATED_AT TIMESTAMP DEFAULT CURRENT TIMESTAMP,
                    UPDATED_AT TIMESTAMP DEFAULT CURRENT TIMESTAMP,
                    PRIMARY KEY (ID)
                )
                """);

            // 인덱스 생성
            stmt.execute("CREATE INDEX IDX_LOAD_TEST_THREAD ON LOAD_TEST(THREAD_ID, CREATED_AT)");

            conn.commit();
            logger.info("DB2 schema created successfully");
        }
    }

    @Override
    public void truncateTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 테이블 TRUNCATE
            stmt.execute("TRUNCATE TABLE LOAD_TEST IMMEDIATE");

            // 시퀀스 재생성 (1부터 다시 시작)
            stmt.execute("DROP SEQUENCE LOAD_TEST_SEQ");
            stmt.execute("CREATE SEQUENCE LOAD_TEST_SEQ START WITH 1 INCREMENT BY 1 CACHE 1000 NO CYCLE ORDER");

            conn.commit();
            logger.info("Table LOAD_TEST truncated and sequence LOAD_TEST_SEQ reset to 1");
        }
    }
}
