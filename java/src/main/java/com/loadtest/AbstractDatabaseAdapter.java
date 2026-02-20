package com.loadtest;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 데이터베이스 어댑터 추상 클래스 - HikariCP 기반
 */
public abstract class AbstractDatabaseAdapter implements DatabaseAdapter {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected HikariDataSource dataSource;
    protected final Random random = new Random();

    @Override
    public void createConnectionPool(DatabaseConfig config) {
        // 외부 JDBC 드라이버 로딩
        if (config.getDriverPath() != null && !config.getDriverPath().isEmpty()) {
            loadExternalDriver(config.getDriverPath());
        }

        // jdbcUrl이 직접 지정된 경우 우선 사용, 그렇지 않으면 빌드
        String jdbcUrl = (config.getJdbcUrl() != null && !config.getJdbcUrl().isEmpty())
                ? config.getJdbcUrl()
                : buildJdbcUrl(config);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(config.getUser());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setDriverClassName(getDriverClassName());

        // 풀 크기 설정
        hikariConfig.setMinimumIdle(config.getMinPoolSize());
        hikariConfig.setMaximumPoolSize(config.getMaxPoolSize());

        // 타임아웃 설정
        hikariConfig.setConnectionTimeout(config.getConnectionTimeoutMs());
        hikariConfig.setValidationTimeout(config.getValidationTimeoutMs());

        // 커넥션 수명 설정
        hikariConfig.setMaxLifetime(TimeUnit.SECONDS.toMillis(config.getMaxLifetimeSeconds()));
        hikariConfig.setIdleTimeout(TimeUnit.SECONDS.toMillis(config.getIdleTimeoutSeconds()));
        hikariConfig.setKeepaliveTime(TimeUnit.SECONDS.toMillis(config.getKeepaliveTimeSeconds()));

        // Leak 감지 설정
        hikariConfig.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(config.getLeakDetectionThresholdSeconds()));

        // 커넥션 테스트 쿼리 설정
        hikariConfig.setConnectionTestQuery(getValidationQuery());

        // 풀 이름 설정
        hikariConfig.setPoolName("HikariPool-" + config.getDbType().toUpperCase());

        // AutoCommit 비활성화 (수동 트랜잭션 관리)
        hikariConfig.setAutoCommit(false);

        // 추가 데이터베이스별 설정
        configureDataSourceProperties(hikariConfig, config);

        logger.info("Initializing HikariCP connection pool");
        logger.info("  - JDBC URL: {}", jdbcUrl);
        logger.info("  - Min Pool Size: {}", config.getMinPoolSize());
        logger.info("  - Max Pool Size: {}", config.getMaxPoolSize());
        logger.info("  - Max Lifetime: {}s", config.getMaxLifetimeSeconds());
        logger.info("  - Idle Timeout: {}s", config.getIdleTimeoutSeconds());
        logger.info("  - Keepalive Time: {}s", config.getKeepaliveTimeSeconds());
        logger.info("  - Leak Detection Threshold: {}s", config.getLeakDetectionThresholdSeconds());

        this.dataSource = new HikariDataSource(hikariConfig);

        logger.info("HikariCP connection pool initialized successfully");
    }

    /**
     * 데이터베이스별 추가 설정
     */
    protected void configureDataSourceProperties(HikariConfig hikariConfig, DatabaseConfig config) {
        // 서브클래스에서 오버라이드
    }

    /**
     * 유효성 검사 쿼리
     */
    protected String getValidationQuery() {
        return "SELECT 1";
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseConnection(Connection connection, boolean isError) {
        if (connection == null) return;
        try {
            if (isError) {
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                    // 롤백 에러 무시
                }
            }
            connection.close();
        } catch (SQLException e) {
            // 커넥션 종료 에러는 무시 (정상적인 종료 상황)
            logger.trace("Connection close error (ignored): {}", e.getMessage());
        }
    }

    @Override
    public void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Closing HikariCP connection pool...");
            dataSource.close();
            logger.info("Connection pool closed");
        }
    }

    @Override
    public Map<String, Object> getPoolStats() {
        Map<String, Object> stats = new HashMap<>();
        if (dataSource != null) {
            stats.put("poolTotal", dataSource.getHikariPoolMXBean().getTotalConnections());
            stats.put("poolActive", dataSource.getHikariPoolMXBean().getActiveConnections());
            stats.put("poolIdle", dataSource.getHikariPoolMXBean().getIdleConnections());
            stats.put("poolPending", dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
        }
        return stats;
    }

    @Override
    public void commit(Connection conn) throws SQLException {
        conn.commit();
    }

    @Override
    public void rollback(Connection conn) {
        if (conn == null) return;
        try {
            conn.rollback();
        } catch (SQLException e) {
            // 롤백 에러는 모두 무시 (이미 에러 복구 경로)
            logger.trace("Rollback error (ignored): {}", e.getMessage());
        }
    }

    @Override
    public long getRandomId(long maxId) {
        if (maxId <= 0) return 0;
        return random.nextLong(1, maxId + 1);
    }

    @Override
    public String generateRandomData(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Override
    public void truncateTable(Connection conn) throws SQLException {
        try (java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE LOAD_TEST");
            conn.commit();
            logger.info("Table LOAD_TEST truncated successfully");
        }
    }

    /**
     * 외부 JDBC 드라이버 JAR을 로드하여 번들된 드라이버를 오버라이드합니다.
     * child-first classloader를 사용하여 외부 JAR의 드라이버 클래스가 우선됩니다.
     */
    protected void loadExternalDriver(String driverPath) {
        File driverFile = new File(driverPath);
        if (!driverFile.exists()) {
            throw new RuntimeException("Driver file not found: " + driverPath);
        }
        if (!driverFile.getName().endsWith(".jar")) {
            throw new RuntimeException("Driver file must be a JAR file: " + driverPath);
        }

        try {
            URL driverUrl = driverFile.toURI().toURL();
            String driverClassName = getDriverClassName();

            // 드라이버 클래스의 패키지 prefix 결정
            String packagePrefix = driverClassName.substring(0, driverClassName.lastIndexOf('.'));

            ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
            if (parentClassLoader == null) {
                parentClassLoader = getClass().getClassLoader();
            }

            DriverOverrideClassLoader classLoader = new DriverOverrideClassLoader(
                    new URL[]{driverUrl}, parentClassLoader, packagePrefix);

            Thread.currentThread().setContextClassLoader(classLoader);

            logger.info("External JDBC driver loaded: {}", driverPath);
            logger.info("  - Driver class: {} (package prefix: {})", driverClassName, packagePrefix);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid driver path: " + driverPath, e);
        }
    }

    /**
     * Child-first ClassLoader for JDBC driver override.
     * 드라이버 패키지에 해당하는 클래스만 child-first로 로드하고,
     * 나머지는 parent-first (기본 위임 모델)을 따릅니다.
     */
    static class DriverOverrideClassLoader extends URLClassLoader {
        private final String driverPackagePrefix;

        DriverOverrideClassLoader(URL[] urls, ClassLoader parent, String driverPackagePrefix) {
            super(urls, parent);
            this.driverPackagePrefix = driverPackagePrefix;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                // 이미 로드된 클래스 확인
                Class<?> c = findLoadedClass(name);
                if (c != null) {
                    if (resolve) resolveClass(c);
                    return c;
                }

                // 드라이버 패키지에 해당하는 클래스는 child-first로 로드
                if (name.startsWith(driverPackagePrefix)) {
                    try {
                        c = findClass(name);
                        if (resolve) resolveClass(c);
                        return c;
                    } catch (ClassNotFoundException e) {
                        // 외부 JAR에 없으면 parent로 fallback
                    }
                }

                // 나머지는 parent-first (기본 동작)
                return super.loadClass(name, resolve);
            }
        }
    }
}
