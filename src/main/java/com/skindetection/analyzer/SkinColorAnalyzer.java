package com.skindetection.analyzer;

import com.skindetection.model.SkinAnalysisData;
import com.skindetection.utils.ImageUtils;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * YCrCb 색공간을 이용한 살색 분석기
 * 히스토그램 분석을 통해 살색 비율과 분포를 계산합니다.
 */
public class SkinColorAnalyzer implements AutoCloseable {

    // 살색 탐지 임계값 (YCrCb 색공간)
    private static final Scalar SKIN_MIN = new Scalar(0, 133, 77);
    private static final Scalar SKIN_MAX = new Scalar(255, 173, 127);

    // 확장된 살색 범위 (더 넓은 범위)
    private static final Scalar SKIN_MIN_EXTENDED = new Scalar(0, 120, 70);
    private static final Scalar SKIN_MAX_EXTENDED = new Scalar(255, 180, 135);

    // 히스토그램 설정
    private static final int HIST_SIZE = 256;
    private static final float[] HIST_RANGE = {0, 256};

    // 분석 임계값들
    private static final double THRESHOLD_25_PERCENT = 0.25;
    private static final double THRESHOLD_40_PERCENT = 0.40;

    public SkinColorAnalyzer() {
        // 초기화 코드 (필요시)
        System.out.println("살색 분석기가 초기화되었습니다.");
    }

    /**
     * 이미지 파일을 분석하여 살색 정보를 추출합니다.
     */
    public SkinAnalysisData analyze(File imageFile) throws Exception {
        long startTime = System.currentTimeMillis();

        // 이미지 로드
        Mat image = loadImage(imageFile);
        if (image.empty()) {
            throw new RuntimeException("이미지를 로드할 수 없습니다: " + imageFile.getPath());
        }

        // YCrCb 색공간으로 변환
        Mat ycrcbImage = new Mat();
        Imgproc.cvtColor(image, ycrcbImage, Imgproc.COLOR_BGR2YCrCb);

        // 살색 마스크 생성
        Mat skinMask = createSkinMask(ycrcbImage);
        Mat skinMaskExtended = createSkinMaskExtended(ycrcbImage);

        // 살색 비율 계산
        double totalPixels = image.total();
        double skinPixels = Core.countNonZero(skinMask);
        double skinPixelsExtended = Core.countNonZero(skinMaskExtended);

        double skinRatio = skinPixels / totalPixels;
        double skinRatioExtended = skinPixelsExtended / totalPixels;

        // 채널별 분석
        List<Mat> ycrcbChannels = new ArrayList<>();
        Core.split(ycrcbImage, ycrcbChannels);

        Mat crChannel = ycrcbChannels.get(1); // Cr 채널
        Mat cbChannel = ycrcbChannels.get(2); // Cb 채널

        // 히스토그램 분석
        HistogramData crHistogram = calculateHistogram(crChannel, skinMask);
        HistogramData cbHistogram = calculateHistogram(cbChannel, skinMask);

        // 채널별 집중도 계산
        double crConcentration = calculateConcentration(crHistogram);
        double cbConcentration = calculateConcentration(cbHistogram);

        // 살색 픽셀의 평균 Cr, Cb 값
        Scalar crMean = Core.mean(crChannel, skinMask);
        Scalar cbMean = Core.mean(cbChannel, skinMask);

        // 임계값 기반 판정
        boolean threshold25Pass = skinRatio >= THRESHOLD_25_PERCENT;
        boolean threshold40Pass = skinRatio >= THRESHOLD_40_PERCENT;

        // 종합 판정
        String finalJudgment = determineFinalJudgment(skinRatio, crConcentration, cbConcentration);

        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;

        // 결과 객체 생성
        SkinAnalysisData result = new SkinAnalysisData();
        result.setImageFile(imageFile);
        result.setTotalSkinRatio(skinRatio * 100); // 백분율로 변환
        result.setSkinRatioExtended(skinRatioExtended * 100);
        result.setCrChannelConcentration(crConcentration * 100);
        result.setCbChannelConcentration(cbConcentration * 100);
        result.setCrSkinPixelRatio(crMean.val[0] / 255.0);
        result.setCbSkinPixelRatio(cbMean.val[0] / 255.0);
        result.setThreshold25Pass(threshold25Pass);
        result.setThreshold40Pass(threshold40Pass);
        result.setFinalJudgment(finalJudgment);
        result.setProcessingTimeMs(processingTime);

        // 추가 통계 정보
        result.setSkinPixelCount((long) skinPixels);
        result.setTotalPixelCount((long) totalPixels);
        result.setCrHistogram(crHistogram);
        result.setCbHistogram(cbHistogram);

        // 메모리 정리
        image.release();
        ycrcbImage.release();
        skinMask.release();
        skinMaskExtended.release();
        for (Mat channel : ycrcbChannels) {
            channel.release();
        }

        return result;
    }

    /**
     * 이미지 파일을 로드합니다.
     */
    private Mat loadImage(File imageFile) {
        Mat image = Imgcodecs.imread(imageFile.getAbsolutePath());

        if (image.empty()) {
            // WebP 등 특수 포맷 처리
            try {
                image = ImageUtils.loadImageAsMat(imageFile);
            } catch (Exception e) {
                System.err.println("이미지 로드 실패: " + e.getMessage());
                return new Mat();
            }
        }

        return image;
    }

    /**
     * 기본 살색 마스크를 생성합니다.
     */
    private Mat createSkinMask(Mat ycrcbImage) {
        Mat mask = new Mat();
        Core.inRange(ycrcbImage, SKIN_MIN, SKIN_MAX, mask);

        // 모폴로지 연산으로 노이즈 제거
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, kernel);
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel);

        kernel.release();
        return mask;
    }

    /**
     * 확장된 살색 마스크를 생성합니다.
     */
    private Mat createSkinMaskExtended(Mat ycrcbImage) {
        Mat mask = new Mat();
        Core.inRange(ycrcbImage, SKIN_MIN_EXTENDED, SKIN_MAX_EXTENDED, mask);

        // 모폴로지 연산으로 노이즈 제거
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, kernel);

        kernel.release();
        return mask;
    }

    /**
     * 지정된 채널의 히스토그램을 계산합니다.
     */
    private HistogramData calculateHistogram(Mat channel, Mat mask) {
        Mat hist = new Mat();
        List<Mat> images = List.of(channel);
        Mat maskToUse = mask.empty() ? new Mat() : mask;

        Imgproc.calcHist(images, new MatOfInt(0), maskToUse, hist,
                new MatOfInt(HIST_SIZE), new MatOfFloat(HIST_RANGE));

        // 히스토그램 데이터를 배열로 변환
        float[] histData = new float[HIST_SIZE];
        hist.get(0, 0, histData);

        // 정규화
        double sum = Core.sumElems(hist).val[0];
        for (int i = 0; i < histData.length; i++) {
            histData[i] = (float) (histData[i] / sum);
        }

        hist.release();

        return new HistogramData(histData);
    }

    /**
     * 히스토그램의 집중도를 계산합니다.
     * 높은 집중도는 특정 색상 범위에 픽셀이 많이 몰려있음을 의미합니다.
     */
    private double calculateConcentration(HistogramData histogram) {
        float[] data = histogram.getData();

        // 엔트로피 기반 집중도 계산
        double entropy = 0.0;
        for (float value : data) {
            if (value > 0) {
                entropy -= value * Math.log(value) / Math.log(2);
            }
        }

        // 최대 엔트로피로 정규화 (0~1 범위)
        double maxEntropy = Math.log(data.length) / Math.log(2);
        double concentration = 1.0 - (entropy / maxEntropy);

        return concentration;
    }

    /**
     * 최종 판정을 결정합니다.
     */
    private String determineFinalJudgment(double skinRatio, double crConcentration, double cbConcentration) {
        // 복합적인 판정 로직
        if (skinRatio >= THRESHOLD_40_PERCENT) {
            if (crConcentration >= 0.3 && cbConcentration >= 0.3) {
                return "살색 감지";
            } else {
                return "살색 의심";
            }
        } else if (skinRatio >= THRESHOLD_25_PERCENT) {
            if (crConcentration >= 0.4 || cbConcentration >= 0.4) {
                return "살색 의심";
            } else {
                return "살색 아님";
            }
        } else {
            return "살색 아님";
        }
    }

    /**
     * 특정 임계값에 대한 상세 분석을 수행합니다.
     */
    public DetailedSkinAnalysis analyzeWithCustomThreshold(File imageFile, double threshold) throws Exception {
        SkinAnalysisData basicAnalysis = analyze(imageFile);

        DetailedSkinAnalysis detailed = new DetailedSkinAnalysis();
        detailed.setBasicAnalysis(basicAnalysis);
        detailed.setCustomThreshold(threshold);
        detailed.setPassesCustomThreshold(basicAnalysis.getTotalSkinRatio() / 100.0 >= threshold);

        // 추가 통계 계산
        detailed.calculateAdditionalStatistics();

        return detailed;
    }

    /**
     * 여러 임계값에 대한 분석 결과를 반환합니다.
     */
    public MultiThresholdAnalysis analyzeMultipleThresholds(File imageFile, double[] thresholds) throws Exception {
        SkinAnalysisData basicAnalysis = analyze(imageFile);

        MultiThresholdAnalysis multiAnalysis = new MultiThresholdAnalysis();
        multiAnalysis.setBasicAnalysis(basicAnalysis);

        double skinRatio = basicAnalysis.getTotalSkinRatio() / 100.0;

        for (double threshold : thresholds) {
            boolean passes = skinRatio >= threshold;
            multiAnalysis.addThresholdResult(threshold, passes);
        }

        return multiAnalysis;
    }

    @Override
    public void close() throws Exception {
        System.out.println("살색 분석기가 종료되었습니다.");
    }

    /**
     * 히스토그램 데이터를 담는 클래스
     */
    public static class HistogramData {
        private final float[] data;

        public HistogramData(float[] data) {
            this.data = data.clone();
        }

        public float[] getData() {
            return data.clone();
        }

        public int getSize() {
            return data.length;
        }

        public float getValueAt(int index) {
            if (index >= 0 && index < data.length) {
                return data[index];
            }
            return 0.0f;
        }

        public int getPeakIndex() {
            int peakIndex = 0;
            float maxValue = data[0];

            for (int i = 1; i < data.length; i++) {
                if (data[i] > maxValue) {
                    maxValue = data[i];
                    peakIndex = i;
                }
            }

            return peakIndex;
        }
    }

    /**
     * 상세 살색 분석 결과
     */
    public static class DetailedSkinAnalysis {
        private SkinAnalysisData basicAnalysis;
        private double customThreshold;
        private boolean passesCustomThreshold;
        private double confidenceScore;
        private String riskLevel;

        // getter/setter 메서드들
        public SkinAnalysisData getBasicAnalysis() { return basicAnalysis; }
        public void setBasicAnalysis(SkinAnalysisData basicAnalysis) { this.basicAnalysis = basicAnalysis; }

        public double getCustomThreshold() { return customThreshold; }
        public void setCustomThreshold(double customThreshold) { this.customThreshold = customThreshold; }

        public boolean isPassesCustomThreshold() { return passesCustomThreshold; }
        public void setPassesCustomThreshold(boolean passesCustomThreshold) { this.passesCustomThreshold = passesCustomThreshold; }

        public double getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

        public void calculateAdditionalStatistics() {
            double skinRatio = basicAnalysis.getTotalSkinRatio() / 100.0;
            double crConcentration = basicAnalysis.getCrChannelConcentration() / 100.0;
            double cbConcentration = basicAnalysis.getCbChannelConcentration() / 100.0;

            // 신뢰도 점수 계산 (0~1)
            this.confidenceScore = (skinRatio + crConcentration + cbConcentration) / 3.0;

            // 위험도 레벨 결정
            if (this.confidenceScore >= 0.7) {
                this.riskLevel = "HIGH";
            } else if (this.confidenceScore >= 0.4) {
                this.riskLevel = "MEDIUM";
            } else {
                this.riskLevel = "LOW";
            }
        }
    }

    /**
     * 다중 임계값 분석 결과
     */
    public static class MultiThresholdAnalysis {
        private SkinAnalysisData basicAnalysis;
        private List<ThresholdResult> thresholdResults;

        public MultiThresholdAnalysis() {
            this.thresholdResults = new ArrayList<>();
        }

        public SkinAnalysisData getBasicAnalysis() { return basicAnalysis; }
        public void setBasicAnalysis(SkinAnalysisData basicAnalysis) { this.basicAnalysis = basicAnalysis; }

        public List<ThresholdResult> getThresholdResults() { return thresholdResults; }

        public void addThresholdResult(double threshold, boolean passes) {
            thresholdResults.add(new ThresholdResult(threshold, passes));
        }

        public static class ThresholdResult {
            private final double threshold;
            private final boolean passes;

            public ThresholdResult(double threshold, boolean passes) {
                this.threshold = threshold;
                this.passes = passes;
            }

            public double getThreshold() { return threshold; }
            public boolean isPasses() { return passes; }
        }
    }
}