package com.loadtest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 테스트 결과 내보내기
 */
public class ResultExporter {
    private static final Logger logger = LoggerFactory.getLogger(ResultExporter.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * CSV 형식으로 내보내기
     */
    public static void exportCsv(String filepath, Map<String, Object> stats,
                                  List<Map<String, Object>> timeSeries,
                                  Map<String, Object> config) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filepath))) {
            // 설정 정보
            writer.println("# Configuration");
            for (Map.Entry<String, Object> entry : config.entrySet()) {
                writer.println("# " + entry.getKey() + "," + entry.getValue());
            }
            writer.println();

            // 최종 통계
            writer.println("# Final Statistics");
            for (Map.Entry<String, Object> entry : stats.entrySet()) {
                writer.println("# " + entry.getKey() + "," + entry.getValue());
            }
            writer.println();

            // 시계열 데이터
            if (!timeSeries.isEmpty()) {
                writer.println("# Time Series Data");
                // 헤더
                Map<String, Object> first = timeSeries.get(0);
                writer.println(String.join(",", first.keySet()));

                // 데이터
                for (Map<String, Object> record : timeSeries) {
                    StringBuilder sb = new StringBuilder();
                    for (Object value : record.values()) {
                        if (sb.length() > 0) sb.append(",");
                        sb.append(value != null ? value.toString() : "");
                    }
                    writer.println(sb);
                }
            }

            logger.info("Results exported to CSV: {}", filepath);
        } catch (IOException e) {
            logger.error("Failed to export CSV: {}", e.getMessage());
        }
    }

    /**
     * JSON 형식으로 내보내기
     */
    public static void exportJson(String filepath, Map<String, Object> stats,
                                   List<Map<String, Object>> timeSeries,
                                   Map<String, Object> config,
                                   Map<String, Double> latencyStats) {
        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, Object> testInfo = new LinkedHashMap<>();
        testInfo.put("timestamp", Instant.now().toString());
        testInfo.put("version", "0.2.2");
        result.put("testInfo", testInfo);

        result.put("configuration", config);
        result.put("finalStatistics", stats);
        result.put("latencyStatistics", latencyStats);
        result.put("timeSeries", timeSeries);

        try (PrintWriter writer = new PrintWriter(new FileWriter(filepath))) {
            writer.println(gson.toJson(result));
            logger.info("Results exported to JSON: {}", filepath);
        } catch (IOException e) {
            logger.error("Failed to export JSON: {}", e.getMessage());
        }
    }
}
