package com.skindetection.model;

import com.skindetection.analyzer.SkinColorAnalyzer.HistogramData;

import java.io.File;

/**
 * 살색 분석 결과 데이터를 담는 모델 클래스
 * YCrCb 색공간 분석 결과와 관련 통계를 포함합니다.
 */
public class SkinAnalysisData {

    // 기본 정보
    private File imageFile;
    private long processingTimeMs;

    // 살색 비율 분석 결과
    private double totalSkinRatio; // 전체 살색 비율 (%)
    private double skinRatioExtended; // 확장 범위 살색 비율 (%)
    private long skinPixelCount; // 살색 픽셀 수
    private long totalPixelCount; // 전체 픽셀 수

    // 채널별 분석 결과
    private double crChannelConcentration; // Cr 채널 집중도 (%)
    private double cbChannelConcentration; // Cb 채널 집중도 (%)
    private double crSkinPixelRatio; // Cr 살색 픽셀 비율 (0~1)
    private double cbSkinPixelRatio; // Cb 살색 픽셀 비율 (0~1)

    // 임계값 기반 판정
    private boolean threshold25Pass; // 25% 임계값 통과 여부
    private boolean threshold40Pass; // 40% 임계값 통과 여부
    private String finalJudgment; // 최종 판정 결과

    // 히스토그램 데이터
    private HistogramData crHistogram;
    private HistogramData cbHistogram;

    // 추가 통계 정보
    private double skinPixelDensity; // 살색 픽셀 밀도
    private double skinRegionCompactness; // 살색 영역 밀집도
    private int skinRegionCount; // 살색 영역 개수
    private double averageSkinRegionSize; // 평균 살색 영역 크기

    /**
     * 기본 생성자
     */
    public SkinAnalysisData() {
        // 기본값 설정
        this.totalSkinRatio = 0.0;
        this.skinRatioExtended = 0.0;
        this.crChannelConcentration = 0.0;
        this.cbChannelConcentration = 0.0;
        this.crSkinPixelRatio = 0.0;
        this.cbSkinPixelRatio = 0.0;
        this.threshold25Pass = false;
        this.threshold40Pass = false;
        this.finalJudgment = "분석 안됨";
    }

    /**
     * 추가 통계를 계산합니다.
     */
    public void calculateAdditionalStatistics() {
        if (totalPixelCount > 0) {
            this.skinPixelDensity = (double) skinPixelCount / totalPixelCount;
        }

        // 살색 영역 밀집도 계산 (Cr, Cb 집중도의 평균)
        this.skinRegionCompactness = (crChannelConcentration + cbChannelConcentration) / 2.0;

        // 평균 살색 영역 크기 추정
        if (skinRegionCount > 0) {
            this.averageSkinRegionSize = (double) skinPixelCount / skinRegionCount;
        }
    }

    /**
     * 신뢰도 점수를 계산합니다 (0~1 범위).
     */
    public double calculateConfidenceScore() {
        double skinRatioScore = Math.min(totalSkinRatio / 100.0, 1.0);
        double concentrationScore = (crChannelConcentration + cbChannelConcentration) / 200.0;
        double pixelRatioScore = (crSkinPixelRatio + cbSkinPixelRatio) / 2.0;

        // 가중 평균으로 종합 점수 계산
        return (skinRatioScore * 0.5) + (concentrationScore * 0.3) + (pixelRatioScore * 0.2);
    }

    /**
     * 위험도 레벨을 결정합니다.
     */
    public String determineRiskLevel() {
        double confidenceScore = calculateConfidenceScore();

        if (confidenceScore >= 0.7) {
            return "HIGH";
        } else if (confidenceScore >= 0.4) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    /**
     * 상세 분석 결과를 문자열로 반환합니다.
     */
    public String getDetailedAnalysisReport() {
        StringBuilder report = new StringBuilder();

        report.append("=== 살색 분석 상세 보고서 ===\n");
        report.append(String.format("파일: %s\n", imageFile != null ? imageFile.getName() : "N/A"));
        report.append(String.format("처리 시간: %d ms\n\n", processingTimeMs));

        report.append("[ 살색 비율 분석 ]\n");
        report.append(String.format("전체 살색 비율: %.2f%%\n", totalSkinRatio));
        report.append(String.format("확장 범위 살색 비율: %.2f%%\n", skinRatioExtended));
        report.append(String.format("살색 픽셀 수: %,d / %,d\n\n", skinPixelCount, totalPixelCount));

        report.append("[ 채널별 분석 ]\n");
        report.append(String.format("Cr 채널 집중도: %.2f%%\n", crChannelConcentration));
        report.append(String.format("Cb 채널 집중도: %.2f%%\n", cbChannelConcentration));
        report.append(String.format("Cr 살색 픽셀 비율: %.3f\n", crSkinPixelRatio));
        report.append(String.format("Cb 살색 픽셀 비율: %.3f\n\n", cbSkinPixelRatio));

        report.append("[ 임계값 판정 ]\n");
        report.append(String.format("25%% 임계값 통과: %s\n", threshold25Pass ? "통과" : "미통과"));
        report.append(String.format("40%% 임계값 통과: %s\n", threshold40Pass ? "통과" : "미통과"));
        report.append(String.format("최종 판정: %s\n\n", finalJudgment));

        report.append("[ 종합 평가 ]\n");
        report.append(String.format("신뢰도 점수: %.3f\n", calculateConfidenceScore()));
        report.append(String.format("위험도 레벨: %s\n", determineRiskLevel()));

        return report.toString();
    }

    /**
     * 간단한 요약 정보를 반환합니다.
     */
    public String getSummary() {
        return String.format("살색비율: %.1f%%, Cr집중도: %.1f%%, Cb집중도: %.1f%%, 판정: %s",
                totalSkinRatio, crChannelConcentration, cbChannelConcentration, finalJudgment);
    }

    /**
     * CSV 출력용 데이터를 반환합니다.
     */
    public String[] toCsvRow() {
        return new String[] {
                imageFile != null ? imageFile.getName() : "",
                String.valueOf(processingTimeMs),
                String.format("%.2f", totalSkinRatio),
                String.format("%.2f", skinRatioExtended),
                String.valueOf(skinPixelCount),
                String.valueOf(totalPixelCount),
                String.format("%.2f", crChannelConcentration),
                String.format("%.2f", cbChannelConcentration),
                String.format("%.3f", crSkinPixelRatio),
                String.format("%.3f", cbSkinPixelRatio),
                String.valueOf(threshold25Pass),
                String.valueOf(threshold40Pass),
                finalJudgment,
                String.format("%.3f", calculateConfidenceScore()),
                determineRiskLevel()
        };
    }

    /**
     * CSV 헤더를 반환합니다.
     */
    public static String[] getCsvHeaders() {
        return new String[] {
                "파일명", "처리시간(ms)", "전체살색비율(%)", "확장살색비율(%)",
                "살색픽셀수", "전체픽셀수", "Cr채널집중도(%)", "Cb채널집중도(%)",
                "Cr살색픽셀비율", "Cb살색픽셀비율", "25%임계값통과", "40%임계값통과",
                "최종판정", "신뢰도점수", "위험도레벨"
        };
    }

    // Getter/Setter 메서드들

    public File getImageFile() {
        return imageFile;
    }

    public void setImageFile(File imageFile) {
        this.imageFile = imageFile;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public double getTotalSkinRatio() {
        return totalSkinRatio;
    }

    public void setTotalSkinRatio(double totalSkinRatio) {
        this.totalSkinRatio = totalSkinRatio;
    }

    public double getSkinRatioExtended() {
        return skinRatioExtended;
    }

    public void setSkinRatioExtended(double skinRatioExtended) {
        this.skinRatioExtended = skinRatioExtended;
    }

    public long getSkinPixelCount() {
        return skinPixelCount;
    }

    public void setSkinPixelCount(long skinPixelCount) {
        this.skinPixelCount = skinPixelCount;
    }

    public long getTotalPixelCount() {
        return totalPixelCount;
    }

    public void setTotalPixelCount(long totalPixelCount) {
        this.totalPixelCount = totalPixelCount;
    }

    public double getCrChannelConcentration() {
        return crChannelConcentration;
    }

    public void setCrChannelConcentration(double crChannelConcentration) {
        this.crChannelConcentration = crChannelConcentration;
    }

    public double getCbChannelConcentration() {
        return cbChannelConcentration;
    }

    public void setCbChannelConcentration(double cbChannelConcentration) {
        this.cbChannelConcentration = cbChannelConcentration;
    }

    public double getCrSkinPixelRatio() {
        return crSkinPixelRatio;
    }

    public void setCrSkinPixelRatio(double crSkinPixelRatio) {
        this.crSkinPixelRatio = crSkinPixelRatio;
    }

    public double getCbSkinPixelRatio() {
        return cbSkinPixelRatio;
    }

    public void setCbSkinPixelRatio(double cbSkinPixelRatio) {
        this.cbSkinPixelRatio = cbSkinPixelRatio;
    }

    public boolean isThreshold25Pass() {
        return threshold25Pass;
    }

    public void setThreshold25Pass(boolean threshold25Pass) {
        this.threshold25Pass = threshold25Pass;
    }

    public boolean isThreshold40Pass() {
        return threshold40Pass;
    }

    public void setThreshold40Pass(boolean threshold40Pass) {
        this.threshold40Pass = threshold40Pass;
    }

    public String getFinalJudgment() {
        return finalJudgment;
    }

    public void setFinalJudgment(String finalJudgment) {
        this.finalJudgment = finalJudgment;
    }

    public HistogramData getCrHistogram() {
        return crHistogram;
    }

    public void setCrHistogram(HistogramData crHistogram) {
        this.crHistogram = crHistogram;
    }

    public HistogramData getCbHistogram() {
        return cbHistogram;
    }

    public void setCbHistogram(HistogramData cbHistogram) {
        this.cbHistogram = cbHistogram;
    }

    public double getSkinPixelDensity() {
        return skinPixelDensity;
    }

    public void setSkinPixelDensity(double skinPixelDensity) {
        this.skinPixelDensity = skinPixelDensity;
    }

    public double getSkinRegionCompactness() {
        return skinRegionCompactness;
    }

    public void setSkinRegionCompactness(double skinRegionCompactness) {
        this.skinRegionCompactness = skinRegionCompactness;
    }

    public int getSkinRegionCount() {
        return skinRegionCount;
    }

    public void setSkinRegionCount(int skinRegionCount) {
        this.skinRegionCount = skinRegionCount;
        calculateAdditionalStatistics();
    }

    public double getAverageSkinRegionSize() {
        return averageSkinRegionSize;
    }

    public void setAverageSkinRegionSize(double averageSkinRegionSize) {
        this.averageSkinRegionSize = averageSkinRegionSize;
    }

    @Override
    public String toString() {
        return String.format("SkinAnalysisData{file='%s', skinRatio=%.2f%%, judgment='%s'}",
                imageFile != null ? imageFile.getName() : "N/A",
                totalSkinRatio, finalJudgment);
    }
}