package com.skindetection.analyzer;

import com.skindetection.utils.ImageUtils;
import com.skindetection.utils.ColorSpaceUtils;
import org.opencv.core.*;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 이미지 전처리를 담당하는 클래스
 * YOLO 및 살색 분석을 위한 다양한 전처리 기능을 제공합니다.
 */
public class ImageProcessor {

    // 전처리 옵션
    private boolean enableNoiseReduction = true;
    private boolean enableContrastEnhancement = false;
    private boolean enableHistogramEqualization = false;
    private double brightnessAdjustment = 0.0; // -100 ~ +100
    private double contrastAdjustment = 1.0; // 0.5 ~ 2.0

    // YOLO 입력 설정
    private int yoloInputSize = 640;
    private boolean maintainAspectRatio = true;
    private Scalar paddingColor = new Scalar(114, 114, 114); // 그레이 패딩

    public ImageProcessor() {
        // 기본 설정으로 초기화
    }

    /**
     * YOLO 모델 입력을 위한 이미지 전처리를 수행합니다.
     */
    public ProcessedImageData preprocessForYOLO(File imageFile) throws Exception {
        Mat originalImage = ImageUtils.loadImageAsMat(imageFile);
        if (originalImage.empty()) {
            throw new RuntimeException("이미지를 로드할 수 없습니다: " + imageFile.getPath());
        }

        ProcessedImageData result = new ProcessedImageData();
        result.originalSize = originalImage.size();

        // 1. 전처리 단계들
        Mat processedImage = originalImage.clone();

        // 노이즈 제거
        if (enableNoiseReduction) {
            processedImage = applyNoiseReduction(processedImage);
        }

        // 밝기/대비 조정
        if (brightnessAdjustment != 0.0 || contrastAdjustment != 1.0) {
            processedImage = adjustBrightnessContrast(processedImage, brightnessAdjustment, contrastAdjustment);
        }

        // 히스토그램 평활화
        if (enableHistogramEqualization) {
            processedImage = applyHistogramEqualization(processedImage);
        }

        // 2. YOLO 입력 형식으로 변환
        Mat yoloInput;
        if (maintainAspectRatio) {
            yoloInput = resizeWithPadding(processedImage, yoloInputSize, paddingColor);
            result.scaleFactor = calculateScaleFactor(result.originalSize, new Size(yoloInputSize, yoloInputSize));
            result.padding = calculatePadding(result.originalSize, yoloInputSize);
        } else {
            yoloInput = new Mat();
            Imgproc.resize(processedImage, yoloInput, new Size(yoloInputSize, yoloInputSize));
            result.scaleFactor = new double[]{
                    (double) yoloInputSize / result.originalSize.width,
                    (double) yoloInputSize / result.originalSize.height
            };
            result.padding = new int[]{0, 0, 0, 0}; // [top, bottom, left, right]
        }

        // 3. 정규화 (0-255 -> 0-1)
        yoloInput.convertTo(yoloInput, CvType.CV_32F, 1.0 / 255.0);

        // 4. BGR -> RGB 변환 (YOLO 모델이 RGB를 기대하는 경우)
        Mat rgbImage = new Mat();
        Imgproc.cvtColor(yoloInput, rgbImage, Imgproc.COLOR_BGR2RGB);

        result.processedImage = rgbImage;
        result.inputSize = new Size(yoloInputSize, yoloInputSize);

        // 메모리 정리
        originalImage.release();
        processedImage.release();
        yoloInput.release();

        return result;
    }

    /**
     * 살색 분석을 위한 이미지 전처리를 수행합니다.
     */
    public Mat preprocessForSkinAnalysis(File imageFile) throws Exception {
        Mat originalImage = ImageUtils.loadImageAsMat(imageFile);
        if (originalImage.empty()) {
            throw new RuntimeException("이미지를 로드할 수 없습니다: " + imageFile.getPath());
        }

        Mat processedImage = originalImage.clone();

        // 1. 노이즈 제거 (살색 분석에 중요)
        if (enableNoiseReduction) {
            processedImage = applyNoiseReduction(processedImage);
        }

        // 2. 밝기/대비 조정 (살색 탐지 정확도 향상)
        if (enableContrastEnhancement) {
            processedImage = enhanceContrast(processedImage);
        }

        // 3. 색상 공간 정규화
        processedImage = normalizeColorSpace(processedImage);

        originalImage.release();

        return processedImage;
    }

    /**
     * 노이즈 제거를 적용합니다.
     */
    private Mat applyNoiseReduction(Mat image) {
        Mat denoised = new Mat();

        // 가우시안 블러로 노이즈 제거
        Imgproc.GaussianBlur(image, denoised, new Size(3, 3), 0.5);

        // 또는 Non-local Means Denoising 사용 (더 고급)
        // Photo.fastNlMeansDenoisingColored(image, denoised, 3, 3, 7, 21);

        return denoised;
    }

    /**
     * 밝기와 대비를 조정합니다.
     */
    private Mat adjustBrightnessContrast(Mat image, double brightness, double contrast) {
        Mat adjusted = new Mat();
        image.convertTo(adjusted, -1, contrast, brightness);
        return adjusted;
    }

    /**
     * 히스토그램 평활화를 적용합니다.
     */
    private Mat applyHistogramEqualization(Mat image) {
        if (image.channels() == 1) {
            // 그레이스케일 이미지
            Mat equalized = new Mat();
            Imgproc.equalizeHist(image, equalized);
            return equalized;
        } else {
            // 컬러 이미지 - LAB 색공간에서 L 채널만 평활화
            Mat lab = new Mat();
            Imgproc.cvtColor(image, lab, Imgproc.COLOR_BGR2Lab);

            List<Mat> labChannels = new ArrayList<>();
            Core.split(lab, labChannels);

            // L 채널 히스토그램 평활화
            Mat equalizedL = new Mat();
            Imgproc.equalizeHist(labChannels.get(0), equalizedL);
            labChannels.set(0, equalizedL);

            // 채널 합치기
            Mat mergedLab = new Mat();
            Core.merge(labChannels, mergedLab);

            // BGR로 다시 변환
            Mat result = new Mat();
            Imgproc.cvtColor(mergedLab, result, Imgproc.COLOR_Lab2BGR);

            // 메모리 정리
            lab.release();
            for (Mat channel : labChannels) {
                channel.release();
            }
            mergedLab.release();

            return result;
        }
    }

    /**
     * 대비를 향상시킵니다.
     */
    private Mat enhanceContrast(Mat image) {
        // CLAHE (Contrast Limited Adaptive Histogram Equalization) 적용
        Mat lab = new Mat();
        Imgproc.cvtColor(image, lab, Imgproc.COLOR_BGR2Lab);

        List<Mat> labChannels = new ArrayList<>();
        Core.split(lab, labChannels);

        // CLAHE 객체 생성
        CLAHE clahe = Imgproc.createCLAHE(2.0, new Size(8, 8));

        // L 채널에 CLAHE 적용
        Mat enhancedL = new Mat();
        clahe.apply(labChannels.get(0), enhancedL);
        labChannels.set(0, enhancedL);

        // 채널 합치기
        Mat mergedLab = new Mat();
        Core.merge(labChannels, mergedLab);

        // BGR로 다시 변환
        Mat result = new Mat();
        Imgproc.cvtColor(mergedLab, result, Imgproc.COLOR_Lab2BGR);

        // 메모리 정리
        lab.release();
        for (Mat channel : labChannels) {
            channel.release();
        }
        mergedLab.release();

        return result;
    }

    /**
     * 색상 공간을 정규화합니다.
     */
    private Mat normalizeColorSpace(Mat image) {
        Mat normalized = new Mat();

        // 각 채널을 개별적으로 정규화
        List<Mat> channels = new ArrayList<>();
        Core.split(image, channels);

        List<Mat> normalizedChannels = new ArrayList<>();
        for (Mat channel : channels) {
            Mat normalizedChannel = new Mat();
            Core.normalize(channel, normalizedChannel, 0, 255, Core.NORM_MINMAX);
            normalizedChannels.add(normalizedChannel);
        }

        Core.merge(normalizedChannels, normalized);

        // 메모리 정리
        for (Mat channel : channels) {
            channel.release();
        }
        for (Mat channel : normalizedChannels) {
            channel.release();
        }

        return normalized;
    }

    /**
     * 종횡비를 유지하면서 패딩을 추가하여 리사이즈합니다.
     */
    private Mat resizeWithPadding(Mat image, int targetSize, Scalar paddingColor) {
        int height = image.rows();
        int width = image.cols();

        // 스케일 계산
        double scale = Math.min((double) targetSize / width, (double) targetSize / height);
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);

        // 리사이즈
        Mat resized = new Mat();
        Imgproc.resize(image, resized, new Size(newWidth, newHeight));

        // 패딩 계산
        int deltaW = targetSize - newWidth;
        int deltaH = targetSize - newHeight;
        int top = deltaH / 2;
        int bottom = deltaH - top;
        int left = deltaW / 2;
        int right = deltaW - left;

        // 패딩 추가
        Mat padded = new Mat();
        Core.copyMakeBorder(resized, padded, top, bottom, left, right,
                Core.BORDER_CONSTANT, paddingColor);

        resized.release();

        return padded;
    }

    /**
     * 스케일 팩터를 계산합니다.
     */
    private double[] calculateScaleFactor(Size originalSize, Size targetSize) {
        double scale = Math.min(targetSize.width / originalSize.width,
                targetSize.height / originalSize.height);
        return new double[]{scale, scale};
    }

    /**
     * 패딩 정보를 계산합니다.
     */
    private int[] calculatePadding(Size originalSize, int targetSize) {
        double scale = Math.min((double) targetSize / originalSize.width,
                (double) targetSize / originalSize.height);

        int newWidth = (int) (originalSize.width * scale);
        int newHeight = (int) (originalSize.height * scale);

        int deltaW = targetSize - newWidth;
        int deltaH = targetSize - newHeight;

        int top = deltaH / 2;
        int bottom = deltaH - top;
        int left = deltaW / 2;
        int right = deltaW - left;

        return new int[]{top, bottom, left, right};
    }

    /**
     * 바운딩 박스 좌표를 원본 이미지 좌표로 변환합니다.
     */
    public float[] convertBoundingBoxToOriginal(float[] bbox, ProcessedImageData processedData) {
        if (bbox == null || bbox.length < 4 || processedData == null) {
            return bbox;
        }

        float[] originalBbox = new float[4];

        if (maintainAspectRatio) {
            // 패딩을 고려한 좌표 변환
            double scale = processedData.scaleFactor[0];
            int[] padding = processedData.padding;

            // 패딩 제거
            originalBbox[0] = (bbox[0] - padding[2]) / (float) scale; // x1
            originalBbox[1] = (bbox[1] - padding[0]) / (float) scale; // y1
            originalBbox[2] = (bbox[2] - padding[2]) / (float) scale; // x2
            originalBbox[3] = (bbox[3] - padding[0]) / (float) scale; // y2
        } else {
            // 단순 스케일링
            originalBbox[0] = bbox[0] / (float) processedData.scaleFactor[0]; // x1
            originalBbox[1] = bbox[1] / (float) processedData.scaleFactor[1]; // y1
            originalBbox[2] = bbox[2] / (float) processedData.scaleFactor[0]; // x2
            originalBbox[3] = bbox[3] / (float) processedData.scaleFactor[1]; // y2
        }

        // 경계 확인
        originalBbox[0] = Math.max(0, Math.min((float) processedData.originalSize.width, originalBbox[0]));
        originalBbox[1] = Math.max(0, Math.min((float) processedData.originalSize.height, originalBbox[1]));
        originalBbox[2] = Math.max(0, Math.min((float) processedData.originalSize.width, originalBbox[2]));
        originalBbox[3] = Math.max(0, Math.min((float) processedData.originalSize.height, originalBbox[3]));

        return originalBbox;
    }

    // Getter/Setter 메서드들

    public boolean isEnableNoiseReduction() { return enableNoiseReduction; }
    public void setEnableNoiseReduction(boolean enableNoiseReduction) { this.enableNoiseReduction = enableNoiseReduction; }

    public boolean isEnableContrastEnhancement() { return enableContrastEnhancement; }
    public void setEnableContrastEnhancement(boolean enableContrastEnhancement) { this.enableContrastEnhancement = enableContrastEnhancement; }

    public boolean isEnableHistogramEqualization() { return enableHistogramEqualization; }
    public void setEnableHistogramEqualization(boolean enableHistogramEqualization) { this.enableHistogramEqualization = enableHistogramEqualization; }

    public double getBrightnessAdjustment() { return brightnessAdjustment; }
    public void setBrightnessAdjustment(double brightnessAdjustment) { this.brightnessAdjustment = brightnessAdjustment; }

    public double getContrastAdjustment() { return contrastAdjustment; }
    public void setContrastAdjustment(double contrastAdjustment) { this.contrastAdjustment = contrastAdjustment; }

    public int getYoloInputSize() { return yoloInputSize; }
    public void setYoloInputSize(int yoloInputSize) { this.yoloInputSize = yoloInputSize; }

    public boolean isMaintainAspectRatio() { return maintainAspectRatio; }
    public void setMaintainAspectRatio(boolean maintainAspectRatio) { this.maintainAspectRatio = maintainAspectRatio; }

    public Scalar getPaddingColor() { return paddingColor; }
    public void setPaddingColor(Scalar paddingColor) { this.paddingColor = paddingColor; }

    /**
     * 전처리된 이미지 데이터를 담는 클래스
     */
    public static class ProcessedImageData {
        public Mat processedImage;
        public Size originalSize;
        public Size inputSize;
        public double[] scaleFactor; // [scaleX, scaleY]
        public int[] padding; // [top, bottom, left, right]

        public void release() {
            if (processedImage != null) {
                processedImage.release();
            }
        }

        @Override
        public String toString() {
            return String.format("ProcessedImageData{original=%s, input=%s, scale=[%.3f,%.3f]}",
                    originalSize, inputSize,
                    scaleFactor != null ? scaleFactor[0] : 0,
                    scaleFactor != null ? scaleFactor[1] : 0);
        }
    }

    /**
     * 전처리 설정을 담는 클래스
     */
    public static class PreprocessingConfig {
        public boolean enableNoiseReduction = true;
        public boolean enableContrastEnhancement = false;
        public boolean enableHistogramEqualization = false;
        public double brightnessAdjustment = 0.0;
        public double contrastAdjustment = 1.0;
        public int yoloInputSize = 640;
        public boolean maintainAspectRatio = true;
        public Scalar paddingColor = new Scalar(114, 114, 114);

        public void applyTo(ImageProcessor processor) {
            processor.setEnableNoiseReduction(enableNoiseReduction);
            processor.setEnableContrastEnhancement(enableContrastEnhancement);
            processor.setEnableHistogramEqualization(enableHistogramEqualization);
            processor.setBrightnessAdjustment(brightnessAdjustment);
            processor.setContrastAdjustment(contrastAdjustment);
            processor.setYoloInputSize(yoloInputSize);
            processor.setMaintainAspectRatio(maintainAspectRatio);
            processor.setPaddingColor(paddingColor);
        }

        @Override
        public String toString() {
            return String.format("PreprocessingConfig{noise=%b, contrast=%b, hist=%b, bright=%.1f, size=%d}",
                    enableNoiseReduction, enableContrastEnhancement, enableHistogramEqualization,
                    brightnessAdjustment, yoloInputSize);
        }
    }
}