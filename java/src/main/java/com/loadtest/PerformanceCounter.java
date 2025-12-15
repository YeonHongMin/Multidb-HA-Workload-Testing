package com.loadtest;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 스레드 안전 성능 카운터 - 1초 이내 측정 지원
 */
public class PerformanceCounter {
    private final AtomicLong totalInserts = new AtomicLong(0);
    private final AtomicLong totalSelects = new AtomicLong(0);
    private final AtomicLong totalUpdates = new AtomicLong(0);
    private final AtomicLong totalDeletes = new AtomicLong(0);
    private final AtomicLong totalTransactions = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong verificationFailures = new AtomicLong(0);
    private final AtomicLong connectionRecreates = new AtomicLong(0);

    private final long startTime;

    // 워밍업 관련
    private volatile Long warmupEndTime = null;
    private final AtomicLong postWarmupTransactions = new AtomicLong(0);
    private volatile Long postWarmupStartTime = null;

    // Sub-second 측정
    private final int subSecondWindowMs;
    private final ConcurrentLinkedDeque<Long> recentTransactions = new ConcurrentLinkedDeque<>();

    // 레이턴시 측정
    private final ConcurrentLinkedDeque<Double> latencies = new ConcurrentLinkedDeque<>();
    private static final int MAX_LATENCIES = 10000;
    private final ReentrantLock latencyLock = new ReentrantLock();

    // 구간별 통계
    private volatile long lastCheckTime;
    private volatile long lastTransactions;
    private volatile long lastInserts;
    private volatile long lastSelects;
    private volatile long lastUpdates;
    private volatile long lastDeletes;
    private volatile long lastErrors;

    // 시계열 데이터
    private final List<Map<String, Object>> timeSeries = Collections.synchronizedList(new ArrayList<>());

    public PerformanceCounter() {
        this(100);
    }

    public PerformanceCounter(int subSecondWindowMs) {
        this.subSecondWindowMs = subSecondWindowMs;
        this.startTime = System.currentTimeMillis();
        this.lastCheckTime = startTime;
    }

    public void setWarmupEndTime(long warmupEndTime) {
        this.warmupEndTime = warmupEndTime;
    }

    public boolean isWarmupPeriod() {
        if (warmupEndTime == null) return false;
        return System.currentTimeMillis() < warmupEndTime;
    }

    /**
     * warmup 기간이 설정되어 있는지 확인 (warmup > 0)
     */
    public boolean hasWarmupConfig() {
        return warmupEndTime != null;
    }

    public void recordTransaction(double latencyMs) {
        long currentTime = System.currentTimeMillis();

        totalTransactions.incrementAndGet();

        // 워밍업 이후 통계
        if (warmupEndTime != null && currentTime >= warmupEndTime) {
            if (postWarmupStartTime == null) {
                postWarmupStartTime = currentTime;
            }
            postWarmupTransactions.incrementAndGet();
        }

        // 최근 트랜잭션 기록 (1초 윈도우)
        recentTransactions.addLast(currentTime);
        long cutoff = currentTime - 1000;
        while (!recentTransactions.isEmpty()) {
            Long first = recentTransactions.peekFirst();
            if (first != null && first < cutoff) {
                recentTransactions.pollFirst();
            } else {
                break;
            }
        }

        // 레이턴시 기록
        if (latencyMs > 0) {
            latencyLock.lock();
            try {
                latencies.addLast(latencyMs);
                while (latencies.size() > MAX_LATENCIES) {
                    latencies.pollFirst();
                }
            } finally {
                latencyLock.unlock();
            }
        }
    }

    public void incrementInsert() {
        incrementInsert(1);
    }

    public void incrementInsert(int count) {
        totalInserts.addAndGet(count);
    }

    public void incrementSelect() {
        totalSelects.incrementAndGet();
    }

    public void incrementUpdate() {
        totalUpdates.incrementAndGet();
    }

    public void incrementDelete() {
        totalDeletes.incrementAndGet();
    }

    public void incrementError() {
        totalErrors.incrementAndGet();
    }

    public void incrementVerificationFailure() {
        verificationFailures.incrementAndGet();
    }

    public void incrementConnectionRecreate() {
        connectionRecreates.incrementAndGet();
    }

    public double getSubSecondTps() {
        long currentTime = System.currentTimeMillis();
        long cutoff = currentTime - 1000;

        // 오래된 항목 제거
        while (!recentTransactions.isEmpty()) {
            Long first = recentTransactions.peekFirst();
            if (first != null && first < cutoff) {
                recentTransactions.pollFirst();
            } else {
                break;
            }
        }

        return recentTransactions.size();
    }

    public double getWindowedTps() {
        return getWindowedTps(subSecondWindowMs);
    }

    public double getWindowedTps(int windowMs) {
        double windowSec = windowMs / 1000.0;
        long currentTime = System.currentTimeMillis();
        long cutoff = currentTime - windowMs;

        int count = 0;
        for (Long t : recentTransactions) {
            if (t >= cutoff) {
                count++;
            }
        }

        return windowSec > 0 ? count / windowSec : 0.0;
    }

    public Map<String, Double> getLatencyStats() {
        latencyLock.lock();
        try {
            if (latencies.isEmpty()) {
                return Map.of("avg", 0.0, "p50", 0.0, "p95", 0.0, "p99", 0.0, "min", 0.0, "max", 0.0);
            }

            List<Double> sorted = new ArrayList<>(latencies);
            Collections.sort(sorted);
            int n = sorted.size();

            double sum = 0;
            for (double lat : sorted) {
                sum += lat;
            }

            return Map.of(
                "avg", sum / n,
                "p50", sorted.get((int) (n * 0.50)),
                "p95", n > 20 ? sorted.get((int) (n * 0.95)) : sorted.get(n - 1),
                "p99", n > 100 ? sorted.get((int) (n * 0.99)) : sorted.get(n - 1),
                "min", sorted.get(0),
                "max", sorted.get(n - 1)
            );
        } finally {
            latencyLock.unlock();
        }
    }

    public Map<String, Object> getIntervalStats() {
        long currentTime = System.currentTimeMillis();
        long currentTransactions = totalTransactions.get();
        long currentInserts = totalInserts.get();
        long currentSelects = totalSelects.get();
        long currentUpdates = totalUpdates.get();
        long currentDeletes = totalDeletes.get();
        long currentErrors = totalErrors.get();

        double intervalTime = (currentTime - lastCheckTime) / 1000.0;
        long intervalTransactions = currentTransactions - lastTransactions;
        long intervalInserts = currentInserts - lastInserts;
        long intervalSelects = currentSelects - lastSelects;
        long intervalUpdates = currentUpdates - lastUpdates;
        long intervalDeletes = currentDeletes - lastDeletes;
        long intervalErrors = currentErrors - lastErrors;

        lastCheckTime = currentTime;
        lastTransactions = currentTransactions;
        lastInserts = currentInserts;
        lastSelects = currentSelects;
        lastUpdates = currentUpdates;
        lastDeletes = currentDeletes;
        lastErrors = currentErrors;

        double intervalTps = intervalTime > 0 ? intervalTransactions / intervalTime : 0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("intervalSeconds", intervalTime);
        stats.put("intervalTransactions", intervalTransactions);
        stats.put("intervalInserts", intervalInserts);
        stats.put("intervalSelects", intervalSelects);
        stats.put("intervalUpdates", intervalUpdates);
        stats.put("intervalDeletes", intervalDeletes);
        stats.put("intervalErrors", intervalErrors);
        stats.put("intervalTps", Math.round(intervalTps * 100.0) / 100.0);
        return stats;
    }

    public void recordTimeSeries(Map<String, Object> poolStats) {
        long currentTime = System.currentTimeMillis();
        Map<String, Object> stats = getStats();
        Map<String, Double> latencyStats = getLatencyStats();

        Map<String, Object> record = new HashMap<>();
        record.put("timestamp", Instant.now().toString());
        record.put("elapsedSeconds", Math.round((currentTime - startTime) / 10.0) / 100.0);
        record.put("totalTransactions", stats.get("totalTransactions"));
        record.put("totalInserts", stats.get("totalInserts"));
        record.put("totalSelects", stats.get("totalSelects"));
        record.put("totalUpdates", stats.get("totalUpdates"));
        record.put("totalDeletes", stats.get("totalDeletes"));
        record.put("totalErrors", stats.get("totalErrors"));
        record.put("realtimeTps", stats.get("realtimeTps"));
        record.put("avgTps", stats.get("avgTps"));
        record.put("latencyAvg", Math.round(latencyStats.get("avg") * 100.0) / 100.0);
        record.put("latencyP95", Math.round(latencyStats.get("p95") * 100.0) / 100.0);
        record.put("latencyP99", Math.round(latencyStats.get("p99") * 100.0) / 100.0);
        record.put("isWarmup", isWarmupPeriod());

        if (poolStats != null) {
            record.putAll(poolStats);
        }

        timeSeries.add(record);
    }

    public Map<String, Object> getStats() {
        long currentTime = System.currentTimeMillis();
        double elapsedTime = (currentTime - startTime) / 1000.0;
        long transactions = totalTransactions.get();
        double avgTps = elapsedTime > 0 ? transactions / elapsedTime : 0;

        double postWarmupTps = 0;
        if (postWarmupStartTime != null) {
            double postWarmupElapsed = (currentTime - postWarmupStartTime) / 1000.0;
            postWarmupTps = postWarmupElapsed > 0 ? postWarmupTransactions.get() / postWarmupElapsed : 0;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalInserts", totalInserts.get());
        stats.put("totalSelects", totalSelects.get());
        stats.put("totalUpdates", totalUpdates.get());
        stats.put("totalDeletes", totalDeletes.get());
        stats.put("totalTransactions", transactions);
        stats.put("totalErrors", totalErrors.get());
        stats.put("verificationFailures", verificationFailures.get());
        stats.put("connectionRecreates", connectionRecreates.get());
        stats.put("elapsedSeconds", Math.round(elapsedTime * 100.0) / 100.0);
        stats.put("avgTps", Math.round(avgTps * 100.0) / 100.0);
        stats.put("realtimeTps", Math.round(getSubSecondTps() * 100.0) / 100.0);
        stats.put("postWarmupTransactions", postWarmupTransactions.get());
        stats.put("postWarmupTps", Math.round(postWarmupTps * 100.0) / 100.0);
        return stats;
    }

    public List<Map<String, Object>> getTimeSeries() {
        return new ArrayList<>(timeSeries);
    }

    // Getters for direct access
    public long getTotalInserts() {
        return totalInserts.get();
    }

    public long getTotalSelects() {
        return totalSelects.get();
    }

    public long getTotalUpdates() {
        return totalUpdates.get();
    }

    public long getTotalDeletes() {
        return totalDeletes.get();
    }

    public long getTotalTransactions() {
        return totalTransactions.get();
    }

    public long getTotalErrors() {
        return totalErrors.get();
    }

    public long getStartTime() {
        return startTime;
    }
}
