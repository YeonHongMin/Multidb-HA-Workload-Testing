package com.loadtest;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Multi-Database Load Tester v2.1 (HikariCP Version)
 *
 * 지원 데이터베이스: Oracle, PostgreSQL, MySQL, SQL Server, Tibero
 */
public class MultiDBLoadTester {
    private static final Logger logger = LoggerFactory.getLogger(MultiDBLoadTester.class);
    private static final String VERSION = "2.1.0";

    private final DatabaseConfig config;
    private DatabaseAdapter dbAdapter;
    private PerformanceCounter perfCounter;
    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);

    public MultiDBLoadTester(DatabaseConfig config) {
        this.config = config;
        this.dbAdapter = createAdapter(config.getDbType());
    }

    private DatabaseAdapter createAdapter(String dbType) {
        return switch (dbType.toLowerCase()) {
            case "oracle" -> new OracleAdapter();
            case "postgresql", "postgres", "pg" -> new PostgreSQLAdapter();
            case "mysql" -> new MySQLAdapter();
            case "sqlserver", "mssql" -> new SQLServerAdapter();
            case "tibero" -> new TiberoAdapter();
            default -> throw new IllegalArgumentException("Unsupported database type: " + dbType);
        };
    }

    public void printDDL() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("DDL for " + config.getDbType().toUpperCase() + " (HikariCP)");
        System.out.println("=".repeat(80));
        System.out.println(dbAdapter.getDDL());
        System.out.println("=".repeat(80) + "\n");
    }

    public void runLoadTest(int threadCount, int durationSeconds, WorkMode mode,
                            boolean skipSchemaSetup, double monitorInterval,
                            int subSecondIntervalMs, int warmupSeconds, int rampUpSeconds,
                            int targetTps, int batchSize,
                            String outputFormat, String outputFile) {

        logger.info("Starting load test: {} threads for {}s (mode: {})",
                threadCount, durationSeconds, mode.getValue());

        // Graceful shutdown handler
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!shutdownRequested.get()) {
                shutdownRequested.set(true);
                logger.info("\n[Shutdown] Graceful shutdown requested. Finishing current transactions...");
            }
        }));

        // Performance counter 초기화
        perfCounter = new PerformanceCounter(subSecondIntervalMs);

        // 커넥션 풀 생성
        dbAdapter.createConnectionPool(config);

        // 스키마 설정
        if (!skipSchemaSetup) {
            logger.info("Setting up database schema...");
            try (Connection conn = dbAdapter.getConnection()) {
                dbAdapter.setupSchema(conn);
            } catch (SQLException e) {
                logger.error("Schema setup failed: {}", e.getMessage());
                System.exit(1);
            }
        }

        // 기존 데이터 확인
        long maxIdCache = 0;
        if (mode == WorkMode.SELECT_ONLY || mode == WorkMode.UPDATE_ONLY ||
            mode == WorkMode.DELETE_ONLY || mode == WorkMode.MIXED) {
            try (Connection conn = dbAdapter.getConnection()) {
                maxIdCache = dbAdapter.getMaxId(conn);
                logger.info("Found {} existing records", maxIdCache);
            } catch (SQLException e) {
                logger.error("Failed to get max ID: {}", e.getMessage());
            }
        }

        // 시간 설정
        Instant now = Instant.now();
        Instant warmupEndTime = warmupSeconds > 0 ? now.plus(warmupSeconds, ChronoUnit.SECONDS) : now;
        Instant endTime = now.plus(durationSeconds + warmupSeconds, ChronoUnit.SECONDS);

        // 워밍업 설정
        if (warmupSeconds > 0) {
            perfCounter.setWarmupEndTime(warmupEndTime.toEpochMilli());
            logger.info("Warmup period: {} seconds", warmupSeconds);
        }

        // Rate limiter
        RateLimiter rateLimiter = targetTps > 0 ? new RateLimiter(targetTps) : null;
        if (targetTps > 0) {
            logger.info("Target TPS: {}", targetTps);
        }

        // 모니터링 스레드
        MonitorThread monitor = new MonitorThread(
                monitorInterval, endTime, dbAdapter, perfCounter, shutdownRequested);
        monitor.start();

        // Ramp-up 지원 워커 실행
        int totalTransactions = 0;
        long rampUpDelayMs = rampUpSeconds > 0 ? (rampUpSeconds * 1000L) / threadCount : 0;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount,
                r -> new Thread(r, "Worker"));
        List<Future<Integer>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < threadCount; i++) {
                // Ramp-up 딜레이
                if (rampUpDelayMs > 0 && i > 0) {
                    Thread.sleep(rampUpDelayMs);
                    if (shutdownRequested.get()) {
                        break;
                    }
                }

                LoadTestWorker worker = new LoadTestWorker(
                        i + 1, dbAdapter, endTime, mode, maxIdCache, batchSize,
                        rateLimiter, perfCounter, shutdownRequested);

                futures.add(executor.submit(worker));
            }

            // 결과 수집
            for (Future<Integer> future : futures) {
                try {
                    totalTransactions += future.get();
                } catch (Exception e) {
                    logger.error("Worker failed: {}", e.getMessage());
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
            try {
                executor.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }

        monitor.stopMonitor();
        try {
            monitor.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 최종 통계 출력
        printFinalStats(threadCount, durationSeconds, totalTransactions, mode,
                warmupSeconds, targetTps, batchSize);

        // 결과 내보내기
        if (outputFormat != null && outputFile != null) {
            exportResults(outputFormat, outputFile, threadCount, durationSeconds, mode);
        }

        dbAdapter.closePool();
    }

    private void printFinalStats(int threadCount, int durationSeconds, int totalTransactions,
                                  WorkMode mode, int warmupSeconds, int targetTps, int batchSize) {
        Map<String, Object> stats = perfCounter.getStats();
        Map<String, Double> latencyStats = perfCounter.getLatencyStats();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("LOAD TEST COMPLETED (HikariCP)");
        System.out.println("=".repeat(80));
        System.out.println("Configuration:");
        System.out.printf("  - Database: %s%n", config.getDbType().toUpperCase());
        System.out.printf("  - Host: %s%n", config.getHost());
        System.out.printf("  - Mode: %s%n", mode.getValue());
        System.out.printf("  - Threads: %d%n", threadCount);
        System.out.printf("  - Duration: %ds%n", durationSeconds);
        if (warmupSeconds > 0) System.out.printf("  - Warmup: %ds%n", warmupSeconds);
        if (targetTps > 0) System.out.printf("  - Target TPS: %d%n", targetTps);
        if (batchSize > 1) System.out.printf("  - Batch Size: %d%n", batchSize);
        System.out.println("-".repeat(80));
        System.out.println("Results:");
        System.out.printf("  - Total Transactions: %,d%n", stats.get("totalTransactions"));
        System.out.printf("  - Total Inserts: %,d%n", stats.get("totalInserts"));
        System.out.printf("  - Total Selects: %,d%n", stats.get("totalSelects"));
        System.out.printf("  - Total Updates: %,d%n", stats.get("totalUpdates"));
        System.out.printf("  - Total Deletes: %,d%n", stats.get("totalDeletes"));
        System.out.printf("  - Total Errors: %,d%n", stats.get("totalErrors"));
        System.out.printf("  - Elapsed Time: %.2fs%n", stats.get("elapsedSeconds"));
        System.out.printf("  - Average TPS: %.2f%n", stats.get("avgTps"));
        System.out.printf("  - Post-Warmup TPS: %.2f%n", stats.get("postWarmupTps"));
        System.out.println("-".repeat(80));
        System.out.println("Latency:");
        System.out.printf("  - Average: %.2fms%n", latencyStats.get("avg"));
        System.out.printf("  - P50: %.2fms%n", latencyStats.get("p50"));
        System.out.printf("  - P95: %.2fms%n", latencyStats.get("p95"));
        System.out.printf("  - P99: %.2fms%n", latencyStats.get("p99"));
        System.out.printf("  - Min: %.2fms%n", latencyStats.get("min"));
        System.out.printf("  - Max: %.2fms%n", latencyStats.get("max"));
        System.out.println("=".repeat(80));
    }

    private void exportResults(String format, String filepath, int threadCount,
                                int durationSeconds, WorkMode mode) {
        Map<String, Object> stats = perfCounter.getStats();
        Map<String, Double> latencyStats = perfCounter.getLatencyStats();
        List<Map<String, Object>> timeSeries = perfCounter.getTimeSeries();

        Map<String, Object> configMap = new LinkedHashMap<>();
        configMap.put("dbType", config.getDbType());
        configMap.put("host", config.getHost());
        configMap.put("mode", mode.getValue());
        configMap.put("threadCount", threadCount);
        configMap.put("durationSeconds", durationSeconds);
        configMap.put("minPoolSize", config.getMinPoolSize());
        configMap.put("maxPoolSize", config.getMaxPoolSize());

        if ("csv".equalsIgnoreCase(format)) {
            ResultExporter.exportCsv(filepath, stats, timeSeries, configMap);
        } else if ("json".equalsIgnoreCase(format)) {
            ResultExporter.exportJson(filepath, stats, timeSeries, configMap, latencyStats);
        }
    }

    public static void main(String[] args) {
        Options options = buildOptions();
        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                printHelp(options);
                return;
            }

            if (cmd.hasOption("version")) {
                System.out.println("Multi-Database Load Tester v" + VERSION + " (HikariCP)");
                return;
            }

            // 필수 옵션 확인
            if (!cmd.hasOption("db-type") || !cmd.hasOption("host") ||
                !cmd.hasOption("user") || !cmd.hasOption("password")) {
                System.err.println("Error: Required options missing: --db-type, --host, --user, --password");
                printHelp(options);
                System.exit(1);
            }

            // 설정 빌드
            DatabaseConfig config = DatabaseConfig.builder()
                    .dbType(cmd.getOptionValue("db-type"))
                    .host(cmd.getOptionValue("host"))
                    .port(cmd.hasOption("port") ? Integer.parseInt(cmd.getOptionValue("port")) : 0)
                    .database(cmd.getOptionValue("database"))
                    .sid(cmd.getOptionValue("sid"))
                    .user(cmd.getOptionValue("user"))
                    .password(cmd.getOptionValue("password"))
                    .minPoolSize(Integer.parseInt(cmd.getOptionValue("min-pool-size", "100")))
                    .maxPoolSize(Integer.parseInt(cmd.getOptionValue("max-pool-size", "200")))
                    .maxLifetimeSeconds(Integer.parseInt(cmd.getOptionValue("max-lifetime", "1800")))
                    .leakDetectionThresholdSeconds(Integer.parseInt(cmd.getOptionValue("leak-detection-threshold", "60")))
                    .idleCheckIntervalSeconds(Integer.parseInt(cmd.getOptionValue("idle-check-interval", "30")))
                    .build();

            MultiDBLoadTester tester = new MultiDBLoadTester(config);

            // DDL 출력 모드
            if (cmd.hasOption("print-ddl")) {
                tester.printDDL();
                return;
            }

            // 테스트 실행
            tester.runLoadTest(
                    Integer.parseInt(cmd.getOptionValue("thread-count", "100")),
                    Integer.parseInt(cmd.getOptionValue("test-duration", "300")),
                    WorkMode.fromString(cmd.getOptionValue("mode", "full")),
                    cmd.hasOption("skip-schema-setup"),
                    Double.parseDouble(cmd.getOptionValue("monitor-interval", "5.0")),
                    Integer.parseInt(cmd.getOptionValue("sub-second-interval", "100")),
                    Integer.parseInt(cmd.getOptionValue("warmup", "0")),
                    Integer.parseInt(cmd.getOptionValue("ramp-up", "0")),
                    Integer.parseInt(cmd.getOptionValue("target-tps", "0")),
                    Integer.parseInt(cmd.getOptionValue("batch-size", "1")),
                    cmd.getOptionValue("output-format"),
                    cmd.getOptionValue("output-file")
            );

        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            printHelp(options);
            System.exit(1);
        }
    }

    private static Options buildOptions() {
        Options options = new Options();

        // 필수 옵션
        options.addOption(Option.builder().longOpt("db-type")
                .hasArg().desc("Database type: oracle, postgresql, mysql, sqlserver, tibero").build());
        options.addOption(Option.builder().longOpt("host")
                .hasArg().desc("Database host").build());
        options.addOption(Option.builder().longOpt("user")
                .hasArg().desc("Database user").build());
        options.addOption(Option.builder().longOpt("password")
                .hasArg().desc("Database password").build());

        // 연결 옵션
        options.addOption(Option.builder().longOpt("port")
                .hasArg().desc("Database port").build());
        options.addOption(Option.builder().longOpt("database")
                .hasArg().desc("Database name (PostgreSQL, MySQL, SQL Server)").build());
        options.addOption(Option.builder().longOpt("sid")
                .hasArg().desc("SID/Service name (Oracle, Tibero)").build());

        // 테스트 옵션
        options.addOption(Option.builder().longOpt("thread-count")
                .hasArg().desc("Number of worker threads (default: 100)").build());
        options.addOption(Option.builder().longOpt("test-duration")
                .hasArg().desc("Test duration in seconds (default: 300)").build());
        options.addOption(Option.builder().longOpt("mode")
                .hasArg().desc("Work mode: full, insert-only, select-only, update-only, delete-only, mixed (default: full)").build());
        options.addOption(Option.builder().longOpt("skip-schema-setup")
                .desc("Skip schema creation").build());

        // 워밍업 및 부하 제어
        options.addOption(Option.builder().longOpt("warmup")
                .hasArg().desc("Warmup period in seconds (default: 0)").build());
        options.addOption(Option.builder().longOpt("ramp-up")
                .hasArg().desc("Ramp-up period in seconds (default: 0)").build());
        options.addOption(Option.builder().longOpt("target-tps")
                .hasArg().desc("Target TPS limit, 0 for unlimited (default: 0)").build());
        options.addOption(Option.builder().longOpt("batch-size")
                .hasArg().desc("Batch insert size (default: 1)").build());

        // 결과 출력
        options.addOption(Option.builder().longOpt("output-format")
                .hasArg().desc("Output format: csv, json").build());
        options.addOption(Option.builder().longOpt("output-file")
                .hasArg().desc("Output file path").build());

        // 모니터링 옵션
        options.addOption(Option.builder().longOpt("monitor-interval")
                .hasArg().desc("Monitor output interval in seconds (default: 5.0)").build());
        options.addOption(Option.builder().longOpt("sub-second-interval")
                .hasArg().desc("Sub-second measurement window in ms (default: 100)").build());

        // 풀 설정
        options.addOption(Option.builder().longOpt("min-pool-size")
                .hasArg().desc("Minimum pool size (default: 100)").build());
        options.addOption(Option.builder().longOpt("max-pool-size")
                .hasArg().desc("Maximum pool size (default: 200)").build());
        options.addOption(Option.builder().longOpt("max-lifetime")
                .hasArg().desc("Connection max lifetime in seconds (default: 1800)").build());
        options.addOption(Option.builder().longOpt("leak-detection-threshold")
                .hasArg().desc("Leak detection threshold in seconds (default: 60)").build());
        options.addOption(Option.builder().longOpt("idle-check-interval")
                .hasArg().desc("Idle connection check interval in seconds (default: 30)").build());

        // 기타
        options.addOption(Option.builder().longOpt("print-ddl")
                .desc("Print DDL and exit").build());
        options.addOption(Option.builder("h").longOpt("help")
                .desc("Show help").build());
        options.addOption(Option.builder("v").longOpt("version")
                .desc("Show version").build());

        return options;
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(100);
        System.out.println();
        System.out.println("Multi-Database Load Tester v" + VERSION + " (HikariCP)");
        System.out.println("High-performance database load testing tool with HikariCP connection pool");
        System.out.println();
        formatter.printHelp("java -jar multi-db-load-tester.jar", options, true);
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Oracle test");
        System.out.println("  java -jar multi-db-load-tester.jar --db-type oracle --host localhost \\");
        System.out.println("      --port 1521 --sid XEPDB1 --user test --password pass \\");
        System.out.println("      --thread-count 100 --test-duration 60");
        System.out.println();
        System.out.println("  # PostgreSQL with warmup and rate limiting");
        System.out.println("  java -jar multi-db-load-tester.jar --db-type postgresql --host localhost \\");
        System.out.println("      --port 5432 --database testdb --user test --password pass \\");
        System.out.println("      --warmup 30 --target-tps 5000 --thread-count 200");
        System.out.println();
    }
}
