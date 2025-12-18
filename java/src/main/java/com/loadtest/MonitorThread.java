package com.loadtest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 모니터링 스레드
 */
public class MonitorThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(MonitorThread.class);

    private final double intervalSeconds;
    private final Instant endTime;
    private final DatabaseAdapter dbAdapter;
    private final PerformanceCounter perfCounter;
    private final AtomicBoolean shutdownRequested;
    private volatile boolean running = true;
    private volatile boolean warmupEndLogged = false;

    public MonitorThread(double intervalSeconds, Instant endTime,
                         DatabaseAdapter dbAdapter, PerformanceCounter perfCounter,
                         AtomicBoolean shutdownRequested) {
        super("Monitor");
        setDaemon(true);
        this.intervalSeconds = intervalSeconds;
        this.endTime = endTime;
        this.dbAdapter = dbAdapter;
        this.perfCounter = perfCounter;
        this.shutdownRequested = shutdownRequested;
    }

    @Override
    public void run() {
        logger.info("[Monitor] Starting (interval: {}s)", intervalSeconds);

        while (running && Instant.now().isBefore(endTime)) {
            if (shutdownRequested.get()) {
                break;
            }

            try {
                Thread.sleep((long) (intervalSeconds * 1000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            Map<String, Object> intervalStats = perfCounter.getIntervalStats();
            Map<String, Object> stats = perfCounter.getStats();
            Map<String, Double> latencyStats = perfCounter.getLatencyStats();
            Map<String, Object> poolStats = dbAdapter.getPoolStats();

            double realtimeTps = perfCounter.getSubSecondTps();

            boolean isWarmup = perfCounter.isWarmupPeriod();
            boolean hasWarmupConfig = perfCounter.hasWarmupConfig();

            // Warmup 종료 시점 로깅 (한 번만, warmup이 설정된 경우에만)
            if (hasWarmupConfig && !isWarmup && !warmupEndLogged) {
                warmupEndLogged = true;
                logger.info("================================================================================");
                logger.info("[Monitor] *** WARMUP COMPLETED *** Starting measurement phase...");
                logger.info("================================================================================");
            }

            // 상태 표시: WARMUP 또는 RUNNING
            String statusIndicator = isWarmup ? "[WARMUP]  " : "[RUNNING] ";

            // Avg TPS 계산:
            // - warmup 중: "-"
            // - warmup 없음 (warmup=0): avgTps 사용
            // - warmup 후: postWarmupTps 사용
            String avgTpsStr;
            if (isWarmup) {
                avgTpsStr = "-";
            } else if (hasWarmupConfig) {
                avgTpsStr = String.format("%.2f", ((Number) stats.get("postWarmupTps")).doubleValue());
            } else {
                avgTpsStr = String.format("%.2f", ((Number) stats.get("avgTps")).doubleValue());
            }

            logger.info(
                "[Monitor] {}TXN: {} | INS: {} | SEL: {} | UPD: {} | DEL: {} | ERR: {} | " +
                "Avg TPS: {} | RT TPS: {} | Lat(p50/p95/p99): {}/{}/{}ms | Pool: {}/{}",
                statusIndicator,
                String.format("%,d", ((Number) intervalStats.get("intervalTransactions")).longValue()),
                String.format("%,d", ((Number) intervalStats.get("intervalInserts")).longValue()),
                String.format("%,d", ((Number) intervalStats.get("intervalSelects")).longValue()),
                String.format("%,d", ((Number) intervalStats.get("intervalUpdates")).longValue()),
                String.format("%,d", ((Number) intervalStats.get("intervalDeletes")).longValue()),
                String.format("%,d", ((Number) intervalStats.get("intervalErrors")).longValue()),
                avgTpsStr,
                String.format("%.2f", realtimeTps),
                String.format("%.1f", latencyStats.get("p50")),
                String.format("%.1f", latencyStats.get("p95")),
                String.format("%.1f", latencyStats.get("p99")),
                poolStats.getOrDefault("poolActive", 0),
                poolStats.getOrDefault("poolTotal", 0)
            );

            // 시계열 데이터 기록
            perfCounter.recordTimeSeries(poolStats);
        }

        logger.info("[Monitor] Stopped");
    }

    public void stopMonitor() {
        running = false;
        interrupt();
    }
}
