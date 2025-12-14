package com.loadtest;

import java.sql.*;

/**
 * Tibero JDBC 어댑터 (HikariCP 기반)
 *
 * Note: Tibero JDBC 드라이버는 Maven Central에 없으므로 수동 설치 필요:
 * mvn install:install-file -Dfile=tibero7-jdbc.jar -DgroupId=com.tmax.tibero -DartifactId=tibero-jdbc -Dversion=7.0 -Dpackaging=jar
 */
public class TiberoAdapter extends AbstractDatabaseAdapter {

    @Override
    public String buildJdbcUrl(DatabaseConfig config) {
        String sid = config.getSid() != null ? config.getSid() : config.getDatabase();
        return String.format("jdbc:tibero:thin:@%s:%d:%s",
                config.getHost(),
                config.getDefaultPort(),
                sid);
    }

    @Override
    public String getDriverClassName() {
        return "com.tmax.tibero.jdbc.TbDriver";
    }

    @Override
    protected String getValidationQuery() {
        return "SELECT 1 FROM DUAL";
    }

    @Override
    public long executeInsert(Connection conn, String threadId, String randomData) throws SQLException {
        String sql = """
            INSERT INTO LOAD_TEST (ID, THREAD_ID, VALUE_COL, RANDOM_DATA, CREATED_AT)
            VALUES (LOAD_TEST_SEQ.NEXTVAL, ?, ?, ?, SYSTIMESTAMP)
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, threadId);
            ps.setString(2, "TEST_" + threadId);
            ps.setString(3, randomData);
            ps.executeUpdate();
        }

        // 생성된 ID 조회
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT LOAD_TEST_SEQ.CURRVAL FROM DUAL")) {
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
            VALUES (LOAD_TEST_SEQ.NEXTVAL, ?, ?, ?, SYSTIMESTAMP)
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
        String sql = "UPDATE LOAD_TEST SET VALUE_COL = ?, UPDATED_AT = SYSTIMESTAMP WHERE ID = ?";
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
        String sql = "SELECT NVL(MAX(ID), 0) FROM LOAD_TEST";
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
            -- Tibero DDL
            CREATE SEQUENCE LOAD_TEST_SEQ START WITH 1 INCREMENT BY 1 CACHE 1000 NOCYCLE ORDER;
            CREATE TABLE LOAD_TEST (
                ID NUMBER(19) NOT NULL,
                THREAD_ID VARCHAR2(50) NOT NULL,
                VALUE_COL VARCHAR2(200),
                RANDOM_DATA VARCHAR2(1000),
                STATUS VARCHAR2(20) DEFAULT 'ACTIVE',
                CREATED_AT TIMESTAMP DEFAULT SYSTIMESTAMP,
                UPDATED_AT TIMESTAMP DEFAULT SYSTIMESTAMP
            ) PARTITION BY HASH (ID) PARTITIONS 16 ENABLE ROW MOVEMENT;
            ALTER TABLE LOAD_TEST ADD CONSTRAINT PK_LOAD_TEST PRIMARY KEY (ID);
            CREATE INDEX IDX_LOAD_TEST_THREAD ON LOAD_TEST(THREAD_ID, CREATED_AT) LOCAL;
            """;
    }

    @Override
    public void setupSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 기존 객체 삭제
            try {
                stmt.execute("DROP SEQUENCE LOAD_TEST_SEQ");
            } catch (SQLException ignored) {}
            try {
                stmt.execute("DROP TABLE LOAD_TEST PURGE");
            } catch (SQLException ignored) {}

            // 시퀀스 생성
            stmt.execute("CREATE SEQUENCE LOAD_TEST_SEQ START WITH 1 INCREMENT BY 1 CACHE 1000 NOCYCLE ORDER");

            // 테이블 생성
            stmt.execute("""
                CREATE TABLE LOAD_TEST (
                    ID NUMBER(19) NOT NULL,
                    THREAD_ID VARCHAR2(50) NOT NULL,
                    VALUE_COL VARCHAR2(200),
                    RANDOM_DATA VARCHAR2(1000),
                    STATUS VARCHAR2(20) DEFAULT 'ACTIVE',
                    CREATED_AT TIMESTAMP DEFAULT SYSTIMESTAMP,
                    UPDATED_AT TIMESTAMP DEFAULT SYSTIMESTAMP
                ) PARTITION BY HASH (ID) PARTITIONS 16 ENABLE ROW MOVEMENT
                """);

            // PK 및 인덱스 생성
            stmt.execute("ALTER TABLE LOAD_TEST ADD CONSTRAINT PK_LOAD_TEST PRIMARY KEY (ID)");
            stmt.execute("CREATE INDEX IDX_LOAD_TEST_THREAD ON LOAD_TEST(THREAD_ID, CREATED_AT) LOCAL");

            conn.commit();
            logger.info("Tibero schema created successfully");
        }
    }
}
