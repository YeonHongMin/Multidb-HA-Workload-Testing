package com.loadtest;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Tibero JDBC 어댑터 (HikariCP 기반)
 *
 * Note: Tibero JDBC 드라이버는 Maven Central에 없으므로 수동 설치 필요:
 * mvn install:install-file -Dfile=tibero7-jdbc.jar -DgroupId=com.tmax.tibero -DartifactId=tibero-jdbc -Dversion=7.0 -Dpackaging=jar
 */
public class TiberoAdapter extends AbstractDatabaseAdapter {

    @Override
    public void createConnectionPool(DatabaseConfig config) {
        // --driver-path가 명시적으로 지정된 경우 부모 로직 그대로
        if (config.getDriverPath() != null && !config.getDriverPath().isEmpty()) {
            super.createConnectionPool(config);
            return;
        }

        try {
            super.createConnectionPool(config);
        } catch (Exception e) {
            if (isDriverVersionMismatch(e)) {
                logger.warn("Tibero 7 driver incompatible with server. Auto-fallback to embedded Tibero 6 driver...");
                closePool();
                String tempDriverPath = extractEmbeddedDriver();
                createPoolWithFallbackDriver(config, tempDriverPath);
                logger.info("Successfully connected using Tibero 6 driver (auto-fallback)");
            } else {
                throw e;
            }
        }
    }

    /**
     * Tibero 6 fallback 드라이버로 직접 커넥션 풀을 생성합니다.
     * HikariCP의 DriverDataSource는 DriverManager/classloader를 통해 드라이버를 로드하므로,
     * shaded된 Tibero 7 드라이버와 충돌합니다. 이를 우회하기 위해
     * child-first classloader로 Tibero 6 드라이버를 직접 로드하고,
     * SimpleDriverDataSource 래퍼를 통해 HikariCP에 DataSource로 전달합니다.
     */
    private void createPoolWithFallbackDriver(DatabaseConfig config, String driverPath) {
        try {
            File driverFile = new File(driverPath);
            URL driverUrl = driverFile.toURI().toURL();

            ClassLoader parentCL = Thread.currentThread().getContextClassLoader();
            if (parentCL == null) parentCL = getClass().getClassLoader();

            // child-first classloader: com.tmax.tibero 패키지 전체를 tibero6 JAR에서 우선 로드
            AbstractDatabaseAdapter.DriverOverrideClassLoader classLoader =
                    new AbstractDatabaseAdapter.DriverOverrideClassLoader(
                            new URL[]{driverUrl}, parentCL, "com.tmax.tibero");

            Class<?> driverClass = Class.forName("com.tmax.tibero.jdbc.TbDriver", true, classLoader);
            Driver tibero6Driver = (Driver) driverClass.getDeclaredConstructor().newInstance();

            logger.info("Tibero 6 driver loaded via child-first classloader: {}", driverPath);

            String jdbcUrl = (config.getJdbcUrl() != null && !config.getJdbcUrl().isEmpty())
                    ? config.getJdbcUrl() : buildJdbcUrl(config);

            Properties connProps = new Properties();
            if (config.getUser() != null) connProps.setProperty("user", config.getUser());
            if (config.getPassword() != null) connProps.setProperty("password", config.getPassword());

            // Tibero 6는 service name URL(@//host:port/service)을 지원하지 않음
            // SID 형식(@host:port:sid)으로 자동 변환
            jdbcUrl = convertToSidUrl(jdbcUrl);

            // HikariCP 설정 — setDataSource()로 DriverDataSource를 완전히 우회
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setDataSource(new SimpleDriverDataSource(tibero6Driver, jdbcUrl, connProps));
            hikariConfig.setMinimumIdle(config.getMinPoolSize());
            hikariConfig.setMaximumPoolSize(config.getMaxPoolSize());
            hikariConfig.setConnectionTimeout(config.getConnectionTimeoutMs());
            hikariConfig.setValidationTimeout(config.getValidationTimeoutMs());
            hikariConfig.setMaxLifetime(TimeUnit.SECONDS.toMillis(config.getMaxLifetimeSeconds()));
            hikariConfig.setIdleTimeout(TimeUnit.SECONDS.toMillis(config.getIdleTimeoutSeconds()));
            hikariConfig.setKeepaliveTime(TimeUnit.SECONDS.toMillis(config.getKeepaliveTimeSeconds()));
            hikariConfig.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(config.getLeakDetectionThresholdSeconds()));
            hikariConfig.setConnectionTestQuery(getValidationQuery());
            hikariConfig.setPoolName("HikariPool-TIBERO");
            hikariConfig.setAutoCommit(false);
            configureDataSourceProperties(hikariConfig, config);

            logger.info("Initializing HikariCP connection pool (Tibero 6 fallback)");
            logger.info("  - JDBC URL: {}", jdbcUrl);
            logger.info("  - Min Pool Size: {}", config.getMinPoolSize());
            logger.info("  - Max Pool Size: {}", config.getMaxPoolSize());

            this.dataSource = new HikariDataSource(hikariConfig);

            logger.info("HikariCP connection pool initialized successfully (Tibero 6 fallback)");
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create pool with Tibero 6 fallback driver: " + ex.getMessage(), ex);
        }
    }

    /**
     * JDBC Driver를 직접 사용하는 간단한 DataSource 래퍼.
     * HikariCP의 DriverDataSource(DriverManager/classloader 기반)를 우회하여
     * child-first classloader로 로드한 드라이버 인스턴스를 직접 사용합니다.
     */
    private static class SimpleDriverDataSource implements javax.sql.DataSource {
        private final Driver driver;
        private final String url;
        private final Properties properties;

        SimpleDriverDataSource(Driver driver, String url, Properties properties) {
            this.driver = driver;
            this.url = url;
            this.properties = properties;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return driver.connect(url, properties);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            Properties props = new Properties(this.properties);
            if (username != null) props.setProperty("user", username);
            if (password != null) props.setProperty("password", password);
            return driver.connect(url, props);
        }

        @Override public java.io.PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(java.io.PrintWriter out) {}
        @Override public void setLoginTimeout(int seconds) {}
        @Override public int getLoginTimeout() { return 0; }
        @Override public java.util.logging.Logger getParentLogger() { return null; }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("Not a wrapper"); }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }

    /**
     * Tibero service name URL을 SID 형식으로 변환합니다.
     * Tibero 6 JDBC 드라이버는 service name URL(@//host:port/service)을 지원하지 않으므로,
     * SID 형식(@host:port:sid)으로 변환합니다.
     *
     * 예: jdbc:tibero:thin:@//192.168.0.153:8629/TPROD
     *   → jdbc:tibero:thin:@192.168.0.153:8629:TPROD
     */
    private String convertToSidUrl(String jdbcUrl) {
        // @//host:port/service 패턴을 @host:port:sid 패턴으로 변환
        if (jdbcUrl != null && jdbcUrl.contains("@//")) {
            String converted = jdbcUrl.replace("@//", "@");
            // 마지막 '/'를 ':'로 변환 (host:port/service → host:port:sid)
            int lastSlash = converted.lastIndexOf('/');
            if (lastSlash > 0) {
                converted = converted.substring(0, lastSlash) + ":" + converted.substring(lastSlash + 1);
            }
            logger.info("Converted service name URL to SID format for Tibero 6: {}", converted);
            return converted;
        }
        return jdbcUrl;
    }

    /**
     * 예외 체인에서 JDBC-12030 (드라이버 버전 불일치) 에러를 검색
     */
    private boolean isDriverVersionMismatch(Throwable e) {
        Throwable current = e;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null && msg.contains("JDBC-12030")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * 내장된 tibero6-jdbc.jar 리소스를 temp 파일로 추출
     */
    private String extractEmbeddedDriver() {
        try (InputStream is = getClass().getResourceAsStream("/drivers/tibero6-jdbc.jar")) {
            if (is == null) {
                throw new RuntimeException("Embedded Tibero 6 driver not found in JAR resources (drivers/tibero6-jdbc.jar)");
            }
            File tempFile = Files.createTempFile("tibero6-jdbc-", ".jar").toFile();
            tempFile.deleteOnExit();
            try (OutputStream os = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
            logger.info("Extracted embedded Tibero 6 driver to: {}", tempFile.getAbsolutePath());
            return tempFile.getAbsolutePath();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to extract embedded Tibero 6 driver", ex);
        }
    }

    @Override
    public String buildJdbcUrl(DatabaseConfig config) {
        // service_name이 설정된 경우 service_name 형식 사용
        if (config.getServiceName() != null && !config.getServiceName().isEmpty()) {
            return String.format("jdbc:tibero:thin:@//%s:%d/%s",
                    config.getHost(),
                    config.getDefaultPort(),
                    config.getServiceName());
        }
        // SID 형식 사용 (기존 방식)
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
            // 테이블 존재 여부 확인
            boolean tableExists = false;
            boolean seqExists = false;

            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME = 'LOAD_TEST'")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    tableExists = true;
                }
            }

            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM USER_SEQUENCES WHERE SEQUENCE_NAME = 'LOAD_TEST_SEQ'")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    seqExists = true;
                }
            }

            if (tableExists && seqExists) {
                logger.info("Tibero schema already exists - reusing existing schema");
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
                    stmt.execute("DROP TABLE LOAD_TEST PURGE");
                } catch (SQLException ignored) {}
            }

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

    @Override
    public void truncateTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 테이블 TRUNCATE
            stmt.execute("TRUNCATE TABLE LOAD_TEST");

            // 시퀀스 재생성 (1부터 다시 시작)
            stmt.execute("DROP SEQUENCE LOAD_TEST_SEQ");
            stmt.execute("CREATE SEQUENCE LOAD_TEST_SEQ START WITH 1 INCREMENT BY 1 CACHE 1000 NOCYCLE ORDER");

            conn.commit();
            logger.info("Table LOAD_TEST truncated and sequence LOAD_TEST_SEQ reset to 1");
        }
    }
}
