package com.loadtest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 부하 테스트 워커 - 전체 기능 지원
 */
public class LoadTestWorker implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(LoadTestWorker.class);

    private final DatabaseAdapter dbAdapter;
    private final Instant endTime;
    private final WorkMode mode;
    private long maxIdCache;
    private final int batchSize;
    private final RateLimiter rateLimiter;
    private final PerformanceCounter perfCounter;
    private final AtomicBoolean shutdownRequested;
    private final String threadName;
    private final Random random = new Random();
    private int transactionCount = 0;

    public LoadTestWorker(int workerId, DatabaseAdapter dbAdapter, Instant endTime,
                          WorkMode mode, long maxIdCache, int batchSize,
                          RateLimiter rateLimiter, PerformanceCounter perfCounter,
                          AtomicBoolean shutdownRequested) {
        this.dbAdapter = dbAdapter;
        this.endTime = endTime;
        this.mode = mode;
        this.maxIdCache = maxIdCache;
        this.batchSize = batchSize;
        this.rateLimiter = rateLimiter;
        this.perfCounter = perfCounter;
        this.shutdownRequested = shutdownRequested;
        this.threadName = String.format("Worker-%04d", workerId);
    }

    private String generateRandomData(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private boolean executeInsert(Connection conn) {
        long startTime = System.nanoTime();
        try {
            String threadId = threadName;
            String randomData = generateRandomData(500);

            if (batchSize > 1) {
                int count = dbAdapter.executeBatchInsert(conn, threadId, batchSize);
                perfCounter.incrementInsert(count);
            } else {
                dbAdapter.executeInsert(conn, threadId, randomData);
                perfCounter.incrementInsert();
            }

            dbAdapter.commit(conn);

            double latencyMs = (System.nanoTime() - startTime) / 1_000_000.0;
            perfCounter.recordTransaction(latencyMs);
            transactionCount++;
            return true;
        } catch (SQLException e) {
            logger.error("[{}] Insert error: {}", threadName, e.getMessage());
            perfCounter.incrementError();
            dbAdapter.rollback(conn);
            return false;
        }
    }

    private boolean executeSelect(Connection conn, long maxId) {
        long startTime = System.nanoTime();
        try {
            dbAdapter.executeRandomSelect(conn, maxId);
            perfCounter.incrementSelect();

            double latencyMs = (System.nanoTime() - startTime) / 1_000_000.0;
            perfCounter.recordTransaction(latencyMs);
            transactionCount++;
            return true;
        } catch (SQLException e) {
            logger.error("[{}] Select error: {}", threadName, e.getMessage());
            perfCounter.incrementError();
            return false;
        }
    }

    private boolean executeUpdate(Connection conn, long maxId) {
        long startTime = System.nanoTime();
        try {
            long recordId = dbAdapter.getRandomId(maxId);
            if (recordId > 0) {
                dbAdapter.executeUpdate(conn, recordId);
                dbAdapter.commit(conn);
            }
            perfCounter.incrementUpdate();

            double latencyMs = (System.nanoTime() - startTime) / 1_000_000.0;
            perfCounter.recordTransaction(latencyMs);
            transactionCount++;
            return true;
        } catch (SQLException e) {
            logger.error("[{}] Update error: {}", threadName, e.getMessage());
            perfCounter.incrementError();
            dbAdapter.rollback(conn);
            return false;
        }
    }

    private boolean executeDelete(Connection conn, long maxId) {
        long startTime = System.nanoTime();
        try {
            long recordId = dbAdapter.getRandomId(maxId);
            if (recordId > 0) {
                dbAdapter.executeDelete(conn, recordId);
                dbAdapter.commit(conn);
            }
            perfCounter.incrementDelete();

            double latencyMs = (System.nanoTime() - startTime) / 1_000_000.0;
            perfCounter.recordTransaction(latencyMs);
            transactionCount++;
            return true;
        } catch (SQLException e) {
            logger.error("[{}] Delete error: {}", threadName, e.getMessage());
            perfCounter.incrementError();
            dbAdapter.rollback(conn);
            return false;
        }
    }

    private boolean executeMixed(Connection conn, long maxId) {
        // INSERT 60%, SELECT 20%, UPDATE 15%, DELETE 5%
        double rand = random.nextDouble();
        if (rand < 0.60) {
            return executeInsert(conn);
        } else if (rand < 0.80) {
            return executeSelect(conn, maxId);
        } else if (rand < 0.95) {
            return executeUpdate(conn, maxId);
        } else {
            return executeDelete(conn, maxId);
        }
    }

    private boolean executeFull(Connection conn) {
        long startTime = System.nanoTime();
        try {
            String threadId = threadName;
            String randomData = generateRandomData(500);

            // INSERT
            long newId = dbAdapter.executeInsert(conn, threadId, randomData);
            perfCounter.incrementInsert();
            dbAdapter.commit(conn);

            // SELECT and verify
            Object[] result = dbAdapter.executeSelect(conn, newId);
            perfCounter.incrementSelect();

            if (result == null || !result[0].equals(newId)) {
                perfCounter.incrementVerificationFailure();
                return false;
            }

            double latencyMs = (System.nanoTime() - startTime) / 1_000_000.0;
            perfCounter.recordTransaction(latencyMs);
            transactionCount++;
            return true;
        } catch (SQLException e) {
            logger.error("[{}] Transaction error: {}", threadName, e.getMessage());
            perfCounter.incrementError();
            dbAdapter.rollback(conn);
            return false;
        }
    }

    @Override
    public Integer call() {
        Thread.currentThread().setName(threadName);
        logger.info("[{}] Starting (mode: {})", threadName, mode.getValue());

        Connection connection = null;
        int consecutiveErrors = 0;
        long maxId = maxIdCache;

        while (Instant.now().isBefore(endTime)) {
            // Graceful shutdown check
            if (shutdownRequested.get()) {
                break;
            }

            // Rate limiting
            if (rateLimiter != null && !rateLimiter.acquire(500)) {
                continue;
            }

            try {
                if (connection == null) {
                    connection = dbAdapter.getConnection();
                    consecutiveErrors = 0;

                    // For modes that need existing data, check maxId
                    if ((mode == WorkMode.SELECT_ONLY || mode == WorkMode.UPDATE_ONLY ||
                         mode == WorkMode.DELETE_ONLY || mode == WorkMode.MIXED) && maxId == 0) {
                        maxId = dbAdapter.getMaxId(connection);
                        if (maxId == 0) {
                            Thread.sleep(1000);
                            continue;
                        }
                    }
                }

                boolean success;
                switch (mode) {
                    case INSERT_ONLY -> success = executeInsert(connection);
                    case SELECT_ONLY -> success = executeSelect(connection, maxId);
                    case UPDATE_ONLY -> success = executeUpdate(connection, maxId);
                    case DELETE_ONLY -> success = executeDelete(connection, maxId);
                    case MIXED -> success = executeMixed(connection, maxId);
                    default -> success = executeFull(connection);
                }

                if (!success) {
                    consecutiveErrors++;
                    if (consecutiveErrors >= 5) {
                        dbAdapter.releaseConnection(connection, true);
                        connection = null;
                        perfCounter.incrementConnectionRecreate();
                        Thread.sleep(500);
                    }
                } else {
                    consecutiveErrors = 0;
                }

            } catch (Exception e) {
                logger.error("[{}] Error: {}", threadName, e.getMessage());
                perfCounter.incrementError();
                if (connection != null) {
                    dbAdapter.releaseConnection(connection, true);
                    connection = null;
                    perfCounter.incrementConnectionRecreate();
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (connection != null) {
            dbAdapter.releaseConnection(connection, false);
        }

        logger.info("[{}] Completed. Transactions: {}", threadName, transactionCount);
        return transactionCount;
    }
}
