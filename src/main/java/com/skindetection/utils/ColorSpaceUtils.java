package com.skindetection.utils;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 색공간 변환을 위한 유틸리티 클래스
 * RGB, YCrCb, HSV, LAB 등 다양한 색공간 변환을 지원합니다.
 */
public class ColorSpaceUtils {

    // 살색 탐지를 위한 YCrCb 범위 상수
    public static final Scalar SKIN_YCRCB_MIN = new Scalar(0, 133, 77);
    public static final Scalar SKIN_YCRCB_MAX = new Scalar(255, 173, 127);

    // 확장된 살색 범위
    public static final Scalar SKIN_YCRCB_MIN_EXTENDED = new Scalar(0, 120, 70);
    public static final Scalar SKIN_YCRCB_MAX_EXTENDED = new Scalar(255, 180, 135);

    // HSV 색공간에서의 살색 범위
    public static final Scalar SKIN_HSV_MIN = new Scalar(0, 10, 60);
    public static final Scalar SKIN_HSV_MAX = new Scalar(20, 150, 255);

    /**
     * RGB를 YCrCb로 변환합니다.
     */
    public static YCrCbColor rgbToYCrCb(int r, int g, int b) {
        // ITU-R BT.601 표준 변환 공식
        double y = 0.299 * r + 0.587 * g + 0.114 * b;
        double cr = 0.5 * r - 0.418688 * g - 0.081312 * b + 128;
        double cb = -0.168736 * r - 0.331264 * g + 0.5 * b + 128;

        return new YCrCbColor(
                (int) Math.round(Math.max(0, Math.min(255, y))),
                (int) Math.round(Math.max(0, Math.min(255, cr))),
                (int) Math.round(Math.max(0, Math.min(255, cb)))
        );
    }

    /**
     * YCrCb를 RGB로 변환합니다.
     */
    public static RGBColor yCrCbToRgb(int y, int cr, int cb) {
        // ITU-R BT.601 표준 역변환 공식
        double r = y + 1.402 * (cr - 128);
        double g = y - 0.344136 * (cb - 128) - 0.714136 * (cr - 128);
        double b = y + 1.772 * (cb - 128);

        return new RGBColor(
                (int) Math.round(Math.max(0, Math.min(255, r))),
                (int) Math.round(Math.max(0, Math.min(255, g))),
                (int) Math.round(Math.max(0, Math.min(255, b)))
        );
    }

    /**
     * RGB를 HSV로 변환합니다.
     */
    public static HSVColor rgbToHsv(int r, int g, int b) {
        float rf = r / 255.0f;
        float gf = g / 255.0f;
        float bf = b / 255.0f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        float h = 0;
        float s = (max == 0) ? 0 : (delta / max);
        float v = max;

        if (delta != 0) {
            if (max == rf) {
                h = ((gf - bf) / delta) % 6;
            } else if (max == gf) {
                h = (bf - rf) / delta + 2;
            } else {
                h = (rf - gf) / delta + 4;
            }
            h *= 60;
            if (h < 0) h += 360;
        }

        return new HSVColor(
                (int) Math.round(h / 2), // OpenCV HSV 범위에 맞춤 (0-179)
                (int) Math.round(s * 255),
                (int) Math.round(v * 255)
        );
    }

    /**
     * HSV를 RGB로 변환합니다.
     */
    public static RGBColor hsvToRgb(int h, int s, int v) {
        float hf = h * 2.0f; // OpenCV 범위에서 일반 범위로 변환
        float sf = s / 255.0f;
        float vf = v / 255.0f;

        float c = vf * sf;
        float x = c * (1 - Math.abs((hf / 60) % 2 - 1));
        float m = vf - c;

        float rf, gf, bf;

        if (hf < 60) {
            rf = c; gf = x; bf = 0;
        } else if (hf < 120) {
            rf = x; gf = c; bf = 0;
        } else if (hf < 180) {
            rf = 0; gf = c; bf = x;
        } else if (hf < 240) {
            rf = 0; gf = x; bf = c;
        } else if (hf < 300) {
            rf = x; gf = 0; bf = c;
        } else {
            rf = c; gf = 0; bf = x;
        }

        return new RGBColor(
                (int) Math.round((rf + m) * 255),
                (int) Math.round((gf + m) * 255),
                (int) Math.round((bf + m) * 255)
        );
    }

    /**
     * RGB 색상이 살색 범위에 있는지 확인합니다 (YCrCb 기준).
     */
    public static boolean isSkinColorYCrCb(int r, int g, int b) {
        YCrCbColor ycrcb = rgbToYCrCb(r, g, b);

        return ycrcb.y >= SKIN_YCRCB_MIN.val[0] && ycrcb.y <= SKIN_YCRCB_MAX.val[0] &&
                ycrcb.cr >= SKIN_YCRCB_MIN.val[1] && ycrcb.cr <= SKIN_YCRCB_MAX.val[1] &&
                ycrcb.cb >= SKIN_YCRCB_MIN.val[2] && ycrcb.cb <= SKIN_YCRCB_MAX.val[2];
    }

    /**
     * RGB 색상이 살색 범위에 있는지 확인합니다 (확장된 YCrCb 기준).
     */
    public static boolean isSkinColorYCrCbExtended(int r, int g, int b) {
        YCrCbColor ycrcb = rgbToYCrCb(r, g, b);

        return ycrcb.y >= SKIN_YCRCB_MIN_EXTENDED.val[0] && ycrcb.y <= SKIN_YCRCB_MAX_EXTENDED.val[0] &&
                ycrcb.cr >= SKIN_YCRCB_MIN_EXTENDED.val[1] && ycrcb.cr <= SKIN_YCRCB_MAX_EXTENDED.val[1] &&
                ycrcb.cb >= SKIN_YCRCB_MIN_EXTENDED.val[2] && ycrcb.cb <= SKIN_YCRCB_MAX_EXTENDED.val[2];
    }

    /**
     * RGB 색상이 살색 범위에 있는지 확인합니다 (HSV 기준).
     */
    public static boolean isSkinColorHSV(int r, int g, int b) {
        HSVColor hsv = rgbToHsv(r, g, b);

        return hsv.h >= SKIN_HSV_MIN.val[0] && hsv.h <= SKIN_HSV_MAX.val[0] &&
                hsv.s >= SKIN_HSV_MIN.val[1] && hsv.s <= SKIN_HSV_MAX.val[1] &&
                hsv.v >= SKIN_HSV_MIN.val[2] && hsv.v <= SKIN_HSV_MAX.val[2];
    }

    /**
     * BufferedImage에서 살색 픽셀 수를 계산합니다.
     */
    public static SkinPixelCount countSkinPixels(BufferedImage image) {
        if (image == null) {
            return new SkinPixelCount(0, 0, 0, 0);
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int totalPixels = width * height;
        int skinPixelsYCrCb = 0;
        int skinPixelsYCrCbExtended = 0;
        int skinPixelsHSV = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                if (isSkinColorYCrCb(r, g, b)) {
                    skinPixelsYCrCb++;
                }

                if (isSkinColorYCrCbExtended(r, g, b)) {
                    skinPixelsYCrCbExtended++;
                }

                if (isSkinColorHSV(r, g, b)) {
                    skinPixelsHSV++;
                }
            }
        }

        return new SkinPixelCount(totalPixels, skinPixelsYCrCb, skinPixelsYCrCbExtended, skinPixelsHSV);
    }

    /**
     * OpenCV Mat을 다른 색공간으로 변환합니다.
     */
    public static Mat convertColorSpace(Mat image, int colorConversion) {
        if (image.empty()) {
            return new Mat();
        }

        Mat converted = new Mat();
        Imgproc.cvtColor(image, converted, colorConversion);
        return converted;
    }

    /**
     * OpenCV Mat에서 YCrCb 색공간으로 변환합니다.
     */
    public static Mat convertToYCrCb(Mat bgrImage) {
        return convertColorSpace(bgrImage, Imgproc.COLOR_BGR2YCrCb);
    }

    /**
     * OpenCV Mat에서 HSV 색공간으로 변환합니다.
     */
    public static Mat convertToHSV(Mat bgrImage) {
        return convertColorSpace(bgrImage, Imgproc.COLOR_BGR2HSV);
    }

    /**
     * OpenCV Mat에서 LAB 색공간으로 변환합니다.
     */
    public static Mat convertToLAB(Mat bgrImage) {
        return convertColorSpace(bgrImage, Imgproc.COLOR_BGR2Lab);
    }

    /**
     * OpenCV Mat에서 살색 마스크를 생성합니다.
     */
    public static Mat createSkinMask(Mat ycrcbImage) {
        return createSkinMask(ycrcbImage, SKIN_YCRCB_MIN, SKIN_YCRCB_MAX);
    }

    /**
     * 사용자 정의 범위로 살색 마스크를 생성합니다.
     */
    public static Mat createSkinMask(Mat ycrcbImage, Scalar minRange, Scalar maxRange) {
        if (ycrcbImage.empty()) {
            return new Mat();
        }

        Mat mask = new Mat();
        Core.inRange(ycrcbImage, minRange, maxRange, mask);

        // 노이즈 제거를 위한 모폴로지 연산
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, kernel);
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel);

        kernel.release();
        return mask;
    }

    /**
     * 색상 히스토그램을 계산합니다.
     */
    public static ColorHistogram calculateColorHistogram(Mat image, Mat mask) {
        if (image.empty()) {
            return new ColorHistogram();
        }

        // 채널 분리
        java.util.List<Mat> channels = new java.util.ArrayList<>();
        Core.split(image, channels);

        // 히스토그램 계산 설정
        MatOfInt histSize = new MatOfInt(256);
        MatOfFloat ranges = new MatOfFloat(0f, 256f);
        boolean accumulate = false;

        // 각 채널별 히스토그램 계산
        Mat hist1 = new Mat();
        Mat hist2 = new Mat();
        Mat hist3 = new Mat();

        if (channels.size() >= 3) {
            Imgproc.calcHist(java.util.Arrays.asList(channels.get(0)), new MatOfInt(0),
                    mask != null ? mask : new Mat(), hist1, histSize, ranges, accumulate);
            Imgproc.calcHist(java.util.Arrays.asList(channels.get(1)), new MatOfInt(0),
                    mask != null ? mask : new Mat(), hist2, histSize, ranges, accumulate);
            Imgproc.calcHist(java.util.Arrays.asList(channels.get(2)), new MatOfInt(0),
                    mask != null ? mask : new Mat(), hist3, histSize, ranges, accumulate);
        }

        // 히스토그램 데이터를 배열로 변환
        float[] hist1Data = new float[256];
        float[] hist2Data = new float[256];
        float[] hist3Data = new float[256];

        if (!hist1.empty()) hist1.get(0, 0, hist1Data);
        if (!hist2.empty()) hist2.get(0, 0, hist2Data);
        if (!hist3.empty()) hist3.get(0, 0, hist3Data);

        // 메모리 정리
        for (Mat channel : channels) {
            channel.release();
        }
        hist1.release();
        hist2.release();
        hist3.release();

        return new ColorHistogram(hist1Data, hist2Data, hist3Data);
    }

    /**
     * 색상 집중도를 계산합니다.
     */
    public static double calculateColorConcentration(float[] histogram) {
        if (histogram == null || histogram.length == 0) {
            return 0.0;
        }

        // 엔트로피 기반 집중도 계산
        double totalCount = 0;
        for (float value : histogram) {
            totalCount += value;
        }

        if (totalCount == 0) {
            return 0.0;
        }

        double entropy = 0.0;
        for (float value : histogram) {
            if (value > 0) {
                double probability = value / totalCount;
                entropy -= probability * Math.log(probability) / Math.log(2);
            }
        }

        // 최대 엔트로피로 정규화 (0~1 범위)
        double maxEntropy = Math.log(histogram.length) / Math.log(2);
        return 1.0 - (entropy / maxEntropy);
    }

    /**
     * YCrCb 색상 클래스
     */
    public static class YCrCbColor {
        public final int y, cr, cb;

        public YCrCbColor(int y, int cr, int cb) {
            this.y = y;
            this.cr = cr;
            this.cb = cb;
        }

        @Override
        public String toString() {
            return String.format("YCrCb(%d, %d, %d)", y, cr, cb);
        }
    }

    /**
     * RGB 색상 클래스
     */
    public static class RGBColor {
        public final int r, g, b;

        public RGBColor(int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public Color toColor() {
            return new Color(r, g, b);
        }

        @Override
        public String toString() {
            return String.format("RGB(%d, %d, %d)", r, g, b);
        }
    }

    /**
     * HSV 색상 클래스
     */
    public static class HSVColor {
        public final int h, s, v;

        public HSVColor(int h, int s, int v) {
            this.h = h;
            this.s = s;
            this.v = v;
        }

        @Override
        public String toString() {
            return String.format("HSV(%d, %d, %d)", h, s, v);
        }
    }

    /**
     * 살색 픽셀 개수 클래스
     */
    public static class SkinPixelCount {
        public final int totalPixels;
        public final int skinPixelsYCrCb;
        public final int skinPixelsYCrCbExtended;
        public final int skinPixelsHSV;

        public SkinPixelCount(int totalPixels, int skinPixelsYCrCb,
                              int skinPixelsYCrCbExtended, int skinPixelsHSV) {
            this.totalPixels = totalPixels;
            this.skinPixelsYCrCb = skinPixelsYCrCb;
            this.skinPixelsYCrCbExtended = skinPixelsYCrCbExtended;
            this.skinPixelsHSV = skinPixelsHSV;
        }

        public double getSkinRatioYCrCb() {
            return totalPixels > 0 ? (double) skinPixelsYCrCb / totalPixels : 0.0;
        }

        public double getSkinRatioYCrCbExtended() {
            return totalPixels > 0 ? (double) skinPixelsYCrCbExtended / totalPixels : 0.0;
        }

        public double getSkinRatioHSV() {
            return totalPixels > 0 ? (double) skinPixelsHSV / totalPixels : 0.0;
        }

        @Override
        public String toString() {
            return String.format("SkinPixelCount{total=%d, YCrCb=%d(%.1f%%), YCrCbExt=%d(%.1f%%), HSV=%d(%.1f%%)}",
                    totalPixels, skinPixelsYCrCb, getSkinRatioYCrCb() * 100,
                    skinPixelsYCrCbExtended, getSkinRatioYCrCbExtended() * 100,
                    skinPixelsHSV, getSkinRatioHSV() * 100);
        }
    }

    /**
     * 색상 히스토그램 클래스
     */
    public static class ColorHistogram {
        public final float[] channel1;
        public final float[] channel2;
        public final float[] channel3;

        public ColorHistogram() {
            this.channel1 = new float[256];
            this.channel2 = new float[256];
            this.channel3 = new float[256];
        }

        public ColorHistogram(float[] channel1, float[] channel2, float[] channel3) {
            this.channel1 = channel1.clone();
            this.channel2 = channel2.clone();
            this.channel3 = channel3.clone();
        }

        public double getConcentration1() {
            return calculateColorConcentration(channel1);
        }

        public double getConcentration2() {
            return calculateColorConcentration(channel2);
        }

        public double getConcentration3() {
            return calculateColorConcentration(channel3);
        }
    }
}