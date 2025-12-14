package com.loadtest;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Token Bucket 기반 Rate Limiter
 */
public class RateLimiter {
    private final int targetTps;
    private double tokens;
    private final double maxTokens;
    private long lastRefill;
    private final ReentrantLock lock = new ReentrantLock();
    private final boolean enabled;

    public RateLimiter(int targetTps) {
        this.targetTps = targetTps;
        this.tokens = targetTps;
        this.maxTokens = targetTps * 2.0;  // 버스트 허용
        this.lastRefill = System.currentTimeMillis();
        this.enabled = targetTps > 0;
    }

    /**
     * 토큰 획득 (Rate Limiting)
     * @param timeoutMs 타임아웃 (밀리초)
     * @return 토큰 획득 성공 여부
     */
    public boolean acquire(long timeoutMs) {
        if (!enabled) {
            return true;
        }

        long startTime = System.currentTimeMillis();

        while (true) {
            lock.lock();
            try {
                // 토큰 리필
                long now = System.currentTimeMillis();
                double elapsed = (now - lastRefill) / 1000.0;
                double refillAmount = elapsed * targetTps;
                tokens = Math.min(maxTokens, tokens + refillAmount);
                lastRefill = now;

                if (tokens >= 1) {
                    tokens -= 1;
                    return true;
                }
            } finally {
                lock.unlock();
            }

            // 타임아웃 체크
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                return false;
            }

            // 짧은 대기
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getTargetTps() {
        return targetTps;
    }
}
