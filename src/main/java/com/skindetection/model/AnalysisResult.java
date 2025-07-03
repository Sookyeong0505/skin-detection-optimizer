package com.skindetection.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 전체 분석 결과를 담는 데이터 모델 클래스
 * YOLO 객체 탐지와 살색 분석 결과를 통합하여 관리합니다.
 */
public class AnalysisResult {

    // 기본 정보
    private long id;
    private LocalDateTime analysisDate;
    private ImageData imageData;
    private String classification; // "정탐" 또는 "오탐"
    private String features; // 오탐 특징 설명
    private String analystNotes; // 분석자 메모

    // 모델 설정 정보
    private String modelName;
    private int inputSize;
    private float confidenceThreshold;

    // YOLO 탐지 결과
    private List<DetectionObject> detectedObjects;
    private int detectedObjectsCount;
    private String detectedClasses;
    private DetectionObject highestConfidenceDetection;

    // 각 클래스별 신뢰도
    private float exposedBreastFConfidence;
    private float exposedBreastMConfidence;
    private float exposedButtocksConfidence;
    private float exposedGenitaliaFConfidence;
    private float exposedGenitaliaMConfidence;

    // 살색 분석 결과
    private SkinAnalysisData skinAnalysisData;

    // 성능 지표
    private long processingStartTime;
    private long totalProcessingTime;
    private double processingDurationSec;

    // 추가 분석 지표
    private double skinConfidenceCombined; // 살색+객체탐지 종합점수
    private String riskLevel; // HIGH, MEDIUM, LOW
    private String filterRecommendation; // BLOCK, REVIEW, ALLOW

    /**
     * 기본 생성자
     */
    public AnalysisResult() {
        this.analysisDate = LocalDateTime.now();
        this.processingStartTime = System.currentTimeMillis();
    }

    /**
     * 생성자
     */
    public AnalysisResult(ImageData imageData, List<DetectionObject> detectedObjects,
                          SkinAnalysisData skinAnalysisData) {
        this();
        this.imageData = imageData;
        this.detectedObjects = detectedObjects;
        this.skinAnalysisData = skinAnalysisData;

        // 탐지 결과 분석
        processDetectionResults();

        // 종합 점수 계산
        calculateCombinedScores();

        // 처리 시간 계산
        this.totalProcessingTime = System.currentTimeMillis();
        this.processingDurationSec = (this.totalProcessingTime - this.processingStartTime) / 1000.0;
    }

    /**
     * 탐지 결과를 분석하여 요약 정보를 생성합니다.
     */
    private void processDetectionResults() {
        if (detectedObjects == null || detectedObjects.isEmpty()) {
            this.detectedObjectsCount = 0;
            this.detectedClasses = "";
            this.highestConfidenceDetection = null;
            resetClassConfidences();
            return;
        }

        this.detectedObjectsCount = detectedObjects.size();

        // 탐지된 클래스 목록 생성
        StringBuilder classesBuilder = new StringBuilder();
        DetectionObject highest = null;
        float maxConfidence = 0.0f;

        // 클래스별 신뢰도 초기화
        resetClassConfidences();

        for (DetectionObject detection : detectedObjects) {
            if (classesBuilder.length() > 0) {
                classesBuilder.append(", ");
            }
            classesBuilder.append(detection.getClassName());

            // 최고 신뢰도 탐지 객체 찾기
            if (detection.getConfidence() > maxConfidence) {
                maxConfidence = detection.getConfidence();
                highest = detection;
            }

            // 클래스별 신뢰도 설정
            setClassConfidence(detection);
        }

        this.detectedClasses = classesBuilder.toString();
        this.highestConfidenceDetection = highest;
    }

    /**
     * 클래스별 신뢰도를 초기화합니다.
     */
    private void resetClassConfidences() {
        this.exposedBreastFConfidence = 0.0f;
        this.exposedBreastMConfidence = 0.0f;
        this.exposedButtocksConfidence = 0.0f;
        this.exposedGenitaliaFConfidence = 0.0f;
        this.exposedGenitaliaMConfidence = 0.0f;
    }

    /**
     * 개별 탐지 객체의 클래스별 신뢰도를 설정합니다.
     */
    private void setClassConfidence(DetectionObject detection) {
        String className = detection.getClassName();
        float confidence = detection.getConfidence();

        switch (className) {
            case "EXPOSED_BREAST_F":
                this.exposedBreastFConfidence = Math.max(this.exposedBreastFConfidence, confidence);
                break;
            case "EXPOSED_BREAST_M":
                this.exposedBreastMConfidence = Math.max(this.exposedBreastMConfidence, confidence);
                break;
            case "EXPOSED_BUTTOCKS":
                this.exposedButtocksConfidence = Math.max(this.exposedButtocksConfidence, confidence);
                break;
            case "EXPOSED_GENITALIA_F":
                this.exposedGenitaliaFConfidence = Math.max(this.exposedGenitaliaFConfidence, confidence);
                break;
            case "EXPOSED_GENITALIA_M":
                this.exposedGenitaliaMConfidence = Math.max(this.exposedGenitaliaMConfidence, confidence);
                break;
        }
    }

    /**
     * 종합 점수들을 계산합니다.
     */
    private void calculateCombinedScores() {
        // 최고 객체 탐지 신뢰도
        float maxObjectConfidence = highestConfidenceDetection != null ?
                highestConfidenceDetection.getConfidence() : 0.0f;

        // 살색 분석 점수 (0~1 범위로 정규화)
        double skinScore = skinAnalysisData != null ?
                skinAnalysisData.getTotalSkinRatio() / 100.0 : 0.0;

        // 종합 점수 계산 (가중 평균)
        this.skinConfidenceCombined = (maxObjectConfidence * 0.6) + (skinScore * 0.4);

        // 위험도 레벨 결정
        this.riskLevel = determineRiskLevel();

        // 필터 권장사항 결정
        this.filterRecommendation = determineFilterRecommendation();
    }

    /**
     * 위험도 레벨을 결정합니다.
     */
    private String determineRiskLevel() {
        if (skinConfidenceCombined >= 0.7) {
            return "HIGH";
        } else if (skinConfidenceCombined >= 0.4) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    /**
     * 필터 권장사항을 결정합니다.
     */
    private String determineFilterRecommendation() {
        if ("HIGH".equals(riskLevel)) {
            return "BLOCK";
        } else if ("MEDIUM".equals(riskLevel)) {
            return "REVIEW";
        } else {
            return "ALLOW";
        }
    }

    /**
     * 분석 결과 요약을 문자열로 반환합니다.
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("=== 분석 결과 요약 ===\n");
        summary.append("파일: ").append(imageData != null ? imageData.getFileName() : "N/A").append("\n");
        summary.append("분석 시간: ").append(analysisDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        summary.append("분류: ").append(classification != null ? classification : "미분류").append("\n");

        if (highestConfidenceDetection != null) {
            summary.append("최고 신뢰도 탐지: ").append(highestConfidenceDetection.getClassName())
                    .append(" (").append(String.format("%.1f%%", highestConfidenceDetection.getConfidence() * 100)).append(")\n");
        }

        if (skinAnalysisData != null) {
            summary.append("살색 비율: ").append(String.format("%.1f%%", skinAnalysisData.getTotalSkinRatio())).append("\n");
        }

        summary.append("종합 점수: ").append(String.format("%.3f", skinConfidenceCombined)).append("\n");
        summary.append("위험도: ").append(riskLevel).append("\n");
        summary.append("권장사항: ").append(filterRecommendation).append("\n");
        summary.append("처리 시간: ").append(String.format("%.3f초", processingDurationSec)).append("\n");

        return summary.toString();
    }

    // Getter/Setter 메서드들

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public LocalDateTime getAnalysisDate() { return analysisDate; }
    public void setAnalysisDate(LocalDateTime analysisDate) { this.analysisDate = analysisDate; }

    public ImageData getImageData() { return imageData; }
    public void setImageData(ImageData imageData) { this.imageData = imageData; }

    public String getClassification() { return classification; }
    public void setClassification(String classification) { this.classification = classification; }

    public String getFeatures() { return features; }
    public void setFeatures(String features) { this.features = features; }

    public String getAnalystNotes() { return analystNotes; }
    public void setAnalystNotes(String analystNotes) { this.analystNotes = analystNotes; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public int getInputSize() { return inputSize; }
    public void setInputSize(int inputSize) { this.inputSize = inputSize; }

    public float getConfidenceThreshold() { return confidenceThreshold; }
    public void setConfidenceThreshold(float confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }

    public List<DetectionObject> getDetectedObjects() { return detectedObjects; }
    public void setDetectedObjects(List<DetectionObject> detectedObjects) {
        this.detectedObjects = detectedObjects;
        processDetectionResults();
    }

    public int getDetectedObjectsCount() { return detectedObjectsCount; }

    public String getDetectedClasses() { return detectedClasses; }

    public DetectionObject getHighestConfidenceDetection() { return highestConfidenceDetection; }

    public float getExposedBreastFConfidence() { return exposedBreastFConfidence; }
    public float getExposedBreastMConfidence() { return exposedBreastMConfidence; }
    public float getExposedButtocksConfidence() { return exposedButtocksConfidence; }
    public float getExposedGenitaliaFConfidence() { return exposedGenitaliaFConfidence; }
    public float getExposedGenitaliaMConfidence() { return exposedGenitaliaMConfidence; }

    public SkinAnalysisData getSkinAnalysisData() { return skinAnalysisData; }
    public void setSkinAnalysisData(SkinAnalysisData skinAnalysisData) {
        this.skinAnalysisData = skinAnalysisData;
        calculateCombinedScores();
    }

    public long getProcessingStartTime() { return processingStartTime; }
    public void setProcessingStartTime(long processingStartTime) { this.processingStartTime = processingStartTime; }

    public long getTotalProcessingTime() { return totalProcessingTime; }
    public void setTotalProcessingTime(long totalProcessingTime) { this.totalProcessingTime = totalProcessingTime; }

    public double getProcessingDurationSec() { return processingDurationSec; }
    public void setProcessingDurationSec(double processingDurationSec) { this.processingDurationSec = processingDurationSec; }

    public double getSkinConfidenceCombined() { return skinConfidenceCombined; }
    public void setSkinConfidenceCombined(double skinConfidenceCombined) { this.skinConfidenceCombined = skinConfidenceCombined; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getFilterRecommendation() { return filterRecommendation; }
    public void setFilterRecommendation(String filterRecommendation) { this.filterRecommendation = filterRecommendation; }

    @Override
    public String toString() {
        return "AnalysisResult{" +
                "id=" + id +
                ", fileName='" + (imageData != null ? imageData.getFileName() : "N/A") + '\'' +
                ", classification='" + classification + '\'' +
                ", detectedObjectsCount=" + detectedObjectsCount +
                ", skinConfidenceCombined=" + skinConfidenceCombined +
                ", riskLevel='" + riskLevel + '\'' +
                ", processingDurationSec=" + processingDurationSec +
                '}';
    }
}