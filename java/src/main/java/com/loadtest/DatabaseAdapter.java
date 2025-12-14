package com.loadtest;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * 데이터베이스 공통 인터페이스
 */
public interface DatabaseAdapter {

    /**
     * HikariCP 커넥션 풀 생성
     */
    void createConnectionPool(DatabaseConfig config);

    /**
     * 커넥션 획득
     */
    Connection getConnection() throws SQLException;

    /**
     * 커넥션 반환 (close)
     */
    void releaseConnection(Connection connection, boolean isError);

    /**
     * 풀 종료
     */
    void closePool();

    /**
     * 풀 상태 조회
     */
    Map<String, Object> getPoolStats();

    /**
     * INSERT 실행
     * @return 생성된 레코드 ID
     */
    long executeInsert(Connection conn, String threadId, String randomData) throws SQLException;

    /**
     * 배치 INSERT 실행
     * @return 삽입된 레코드 수
     */
    int executeBatchInsert(Connection conn, String threadId, int batchSize) throws SQLException;

    /**
     * SELECT 실행
     */
    Object[] executeSelect(Connection conn, long recordId) throws SQLException;

    /**
     * 랜덤 SELECT 실행
     */
    Object[] executeRandomSelect(Connection conn, long maxId) throws SQLException;

    /**
     * UPDATE 실행
     */
    boolean executeUpdate(Connection conn, long recordId) throws SQLException;

    /**
     * DELETE 실행
     */
    boolean executeDelete(Connection conn, long recordId) throws SQLException;

    /**
     * 최대 ID 조회
     */
    long getMaxId(Connection conn) throws SQLException;

    /**
     * 랜덤 ID 생성
     */
    long getRandomId(long maxId);

    /**
     * 커밋
     */
    void commit(Connection conn) throws SQLException;

    /**
     * 롤백
     */
    void rollback(Connection conn);

    /**
     * DDL 스크립트 반환
     */
    String getDDL();

    /**
     * 스키마 설정
     */
    void setupSchema(Connection conn) throws SQLException;

    /**
     * JDBC URL 생성
     */
    String buildJdbcUrl(DatabaseConfig config);

    /**
     * 드라이버 클래스명
     */
    String getDriverClassName();

    /**
     * 랜덤 데이터 생성
     */
    default String generateRandomData(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
