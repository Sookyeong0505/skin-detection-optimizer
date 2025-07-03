package com.skindetection.data;

import com.skindetection.model.AnalysisResult;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 분석 결과 데이터를 수집하고 관리하는 클래스
 * 메모리와 파일 시스템에 데이터를 저장하며, 통계 정보를 제공합니다.
 */
public class DataCollector {

    // 데이터 저장
    private final List<AnalysisResult> allResults;
    private final Map<String, List<AnalysisResult>> resultsByClassification;
    private final Map<String, List<AnalysisResult>> resultsByModel;
    private final AtomicLong nextId;

    // 세션 정보
    private final LocalDateTime sessionStartTime;
    private String sessionId;

    // 통계 캐시
    private volatile StatisticsSummary cachedStatistics;
    private volatile long lastStatisticsUpdate;

    // 설정
    private static final String DATA_DIR = "data/results";
    private static final String BACKUP_DIR = "data/backup";
    private static final long STATISTICS_CACHE_DURATION = 5000; // 5초

    public DataCollector() {
        this.allResults = Collections.synchronizedList(new ArrayList<>());
        this.resultsByClassification = new ConcurrentHashMap<>();
        this.resultsByModel = new ConcurrentHashMap<>();
        this.nextId = new AtomicLong(1);
        this.sessionStartTime = LocalDateTime.now();
        this.sessionId = generateSessionId();

        // 디렉토리 생성
        createDirectories();

        // 기존 데이터 로드 (선택적)
        loadExistingData();

        System.out.println("데이터 수집기가 초기화되었습니다. 세션 ID: " + sessionId);
    }

    /**
     * 세션 ID를 생성합니다.
     */
    private String generateSessionId() {
        return "SESSION_" + sessionStartTime.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    /**
     * 필요한 디렉토리들을 생성합니다.
     */
    private void createDirectories() {
        new File(DATA_DIR).mkdirs();
        new File(BACKUP_DIR).mkdirs();
    }

    /**
     * 기존 저장된 데이터를 로드합니다.
     */
    private void loadExistingData() {
        // 필요시 구현 (JSON, 직렬화 등을 통한 데이터 복원)
        System.out.println("기존 데이터 로드를 건너뜁니다.");
    }

    /**
     * 분석 결과를 추가합니다.
     */
    public synchronized void addResult(AnalysisResult result) {
        if (result == null) {
            throw new IllegalArgumentException("결과가 null일 수 없습니다.");
        }

        // ID 할당
        result.setId(nextId.getAndIncrement());

        // 메인 리스트에 추가
        allResults.add(result);

        // 분류별 그룹화
        String classification = result.getClassification();
        if (classification != null) {
            resultsByClassification.computeIfAbsent(classification, k -> new ArrayList<>()).add(result);
        }

        // 모델별 그룹화
        String model = result.getModelName();
        if (model != null) {
            resultsByModel.computeIfAbsent(model, k -> new ArrayList<>()).add(result);
        }

        // 통계 캐시 무효화
        invalidateStatisticsCache();

        // 자동 백업 (옵션)
        if (allResults.size() % 10 == 0) {
            performAutoBackup();
        }

        System.out.println("결과 추가됨: ID=" + result.getId() + ", 총 " + allResults.size() + "개");
    }

    /**
     * 여러 결과를 한번에 추가합니다.
     */
    public synchronized void addResults(List<AnalysisResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }

        for (AnalysisResult result : results) {
            addResult(result);
        }
    }

    /**
     * 특정 결과를 제거합니다.
     */
    public synchronized boolean removeResult(long id) {
        AnalysisResult toRemove = null;

        for (AnalysisResult result : allResults) {
            if (result.getId() == id) {
                toRemove = result;
                break;
            }
        }

        if (toRemove != null) {
            allResults.remove(toRemove);

            // 그룹에서도 제거
            String classification = toRemove.getClassification();
            if (classification != null) {
                List<AnalysisResult> classificationList = resultsByClassification.get(classification);
                if (classificationList != null) {
                    classificationList.remove(toRemove);
                }
            }

            String model = toRemove.getModelName();
            if (model != null) {
                List<AnalysisResult> modelList = resultsByModel.get(model);
                if (modelList != null) {
                    modelList.remove(toRemove);
                }
            }

            invalidateStatisticsCache();
            return true;
        }

        return false;
    }

    /**
     * 모든 결과를 반환합니다.
     */
    public List<AnalysisResult> getAllResults() {
        return new ArrayList<>(allResults);
    }

    /**
     * 분류별 결과를 반환합니다.
     */
    public List<AnalysisResult> getResultsByClassification(String classification) {
        List<AnalysisResult> results = resultsByClassification.get(classification);
        return results != null ? new ArrayList<>(results) : new ArrayList<>();
    }

    /**
     * 모델별 결과를 반환합니다.
     */
    public List<AnalysisResult> getResultsByModel(String model) {
        List<AnalysisResult> results = resultsByModel.get(model);
        return results != null ? new ArrayList<>(results) : new ArrayList<>();
    }

    /**
     * 결과 개수를 반환합니다.
     */
    public int getResultCount() {
        return allResults.size();
    }

    /**
     * 특정 분류의 결과 개수를 반환합니다.
     */
    public int getResultCount(String classification) {
        List<AnalysisResult> results = resultsByClassification.get(classification);
        return results != null ? results.size() : 0;
    }

    /**
     * 통계 요약을 반환합니다.
     */
    public StatisticsSummary getStatistics() {
        long currentTime = System.currentTimeMillis();

        if (cachedStatistics == null ||
                (currentTime - lastStatisticsUpdate) > STATISTICS_CACHE_DURATION) {

            cachedStatistics = calculateStatistics();
            lastStatisticsUpdate = currentTime;
        }

        return cachedStatistics;
    }

    /**
     * 통계를 계산합니다.
     */
    private StatisticsSummary calculateStatistics() {
        StatisticsSummary stats = new StatisticsSummary();

        if (allResults.isEmpty()) {
            return stats;
        }

        // 기본 통계
        stats.totalCount = allResults.size();
        stats.truePositiveCount = getResultCount("정탐");
        stats.falsePositiveCount = getResultCount("오탐");

        // 모델별 통계
        stats.modelCounts = new HashMap<>();
        for (Map.Entry<String, List<AnalysisResult>> entry : resultsByModel.entrySet()) {
            stats.modelCounts.put(entry.getKey(), entry.getValue().size());
        }

        // 신뢰도 통계
        DoubleSummaryStatistics confidenceStats = allResults.stream()
                .mapToDouble(AnalysisResult::getSkinConfidenceCombined)
                .summaryStatistics();

        stats.averageConfidence = confidenceStats.getAverage();
        stats.minConfidence = confidenceStats.getMin();
        stats.maxConfidence = confidenceStats.getMax();

        // 살색 비율 통계
        DoubleSummaryStatistics skinRatioStats = allResults.stream()
                .filter(r -> r.getSkinAnalysisData() != null)
                .mapToDouble(r -> r.getSkinAnalysisData().getTotalSkinRatio())
                .summaryStatistics();

        stats.averageSkinRatio = skinRatioStats.getAverage();
        stats.minSkinRatio = skinRatioStats.getMin();
        stats.maxSkinRatio = skinRatioStats.getMax();

        // 처리 시간 통계
        DoubleSummaryStatistics processingTimeStats = allResults.stream()
                .mapToDouble(AnalysisResult::getProcessingDurationSec)
                .summaryStatistics();

        stats.averageProcessingTime = processingTimeStats.getAverage();
        stats.totalProcessingTime = processingTimeStats.getSum();

        // 위험도별 통계
        stats.riskLevelCounts = new HashMap<>();
        allResults.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        AnalysisResult::getRiskLevel,
                        java.util.stream.Collectors.counting()))
                .forEach((riskLevel, count) -> stats.riskLevelCounts.put(riskLevel, count.intValue()));

        return stats;
    }

    /**
     * 통계 캐시를 무효화합니다.
     */
    private void invalidateStatisticsCache() {
        cachedStatistics = null;
    }

    /**
     * 자동 백업을 수행합니다.
     */
    private void performAutoBackup() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupFileName = String.format("backup_%s_%s.json", sessionId, timestamp);
            File backupFile = new File(BACKUP_DIR, backupFileName);

            // 간단한 JSON 형태로 저장 (실제로는 Jackson 등의 라이브러리 사용 권장)
            saveToJsonFile(backupFile);

            System.out.println("자동 백업 완료: " + backupFile.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("자동 백업 실패: " + e.getMessage());
        }
    }

    /**
     * JSON 파일로 저장합니다.
     */
    private void saveToJsonFile(File file) throws IOException {
        // 간단한 JSON 형태로 저장 (실제 프로젝트에서는 Jackson 등 사용)
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("{");
            writer.println("  \"sessionId\": \"" + sessionId + "\",");
            writer.println("  \"sessionStartTime\": \"" + sessionStartTime + "\",");
            writer.println("  \"totalResults\": " + allResults.size() + ",");
            writer.println("  \"results\": [");

            for (int i = 0; i < allResults.size(); i++) {
                AnalysisResult result = allResults.get(i);
                writer.println("    {");
                writer.println("      \"id\": " + result.getId() + ",");
                writer.println("      \"fileName\": \"" +
                        (result.getImageData() != null ? result.getImageData().getFileName() : "N/A") + "\",");
                writer.println("      \"classification\": \"" + result.getClassification() + "\",");
                writer.println("      \"confidence\": " + result.getSkinConfidenceCombined() + ",");
                writer.println("      \"riskLevel\": \"" + result.getRiskLevel() + "\"");
                writer.print("    }");
                if (i < allResults.size() - 1) {
                    writer.println(",");
                } else {
                    writer.println();
                }
            }

            writer.println("  ]");
            writer.println("}");
        }
    }

    /**
     * 데이터를 초기화합니다.
     */
    public synchronized void clearAllData() {
        allResults.clear();
        resultsByClassification.clear();
        resultsByModel.clear();
        nextId.set(1);
        invalidateStatisticsCache();

        System.out.println("모든 데이터가 초기화되었습니다.");
    }

    /**
     * 세션 정보를 반환합니다.
     */
    public String getSessionInfo() {
        return String.format("세션 ID: %s, 시작 시간: %s, 수집된 데이터: %d개",
                sessionId, sessionStartTime, allResults.size());
    }

    /**
     * 통계 요약 클래스
     */
    public static class StatisticsSummary {
        public int totalCount = 0;
        public int truePositiveCount = 0;
        public int falsePositiveCount = 0;
        public Map<String, Integer> modelCounts = new HashMap<>();
        public Map<String, Integer> riskLevelCounts = new HashMap<>();

        public double averageConfidence = 0.0;
        public double minConfidence = 0.0;
        public double maxConfidence = 0.0;

        public double averageSkinRatio = 0.0;
        public double minSkinRatio = 0.0;
        public double maxSkinRatio = 0.0;

        public double averageProcessingTime = 0.0;
        public double totalProcessingTime = 0.0;

        public double getTruePositiveRate() {
            return totalCount > 0 ? (double) truePositiveCount / totalCount : 0.0;
        }

        public double getFalsePositiveRate() {
            return totalCount > 0 ? (double) falsePositiveCount / totalCount : 0.0;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== 통계 요약 ===\n");
            sb.append("총 데이터 수: ").append(totalCount).append("\n");
            sb.append("정탐: ").append(truePositiveCount).append(" (").append(String.format("%.1f%%", getTruePositiveRate() * 100)).append(")\n");
            sb.append("오탐: ").append(falsePositiveCount).append(" (").append(String.format("%.1f%%", getFalsePositiveRate() * 100)).append(")\n");
            sb.append("평균 신뢰도: ").append(String.format("%.3f", averageConfidence)).append("\n");
            sb.append("평균 살색 비율: ").append(String.format("%.1f%%", averageSkinRatio)).append("\n");
            sb.append("평균 처리 시간: ").append(String.format("%.3f초", averageProcessingTime)).append("\n");

            if (!modelCounts.isEmpty()) {
                sb.append("모델별 데이터 수:\n");
                modelCounts.forEach((model, count) ->
                        sb.append("  ").append(model).append(": ").append(count).append("\n"));
            }

            if (!riskLevelCounts.isEmpty()) {
                sb.append("위험도별 데이터 수:\n");
                riskLevelCounts.forEach((risk, count) ->
                        sb.append("  ").append(risk).append(": ").append(count).append("\n"));
            }

            return sb.toString();
        }
    }
}