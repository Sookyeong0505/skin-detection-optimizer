package com.skindetection.utils;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * 이미지 처리를 위한 유틸리티 클래스
 * OpenCV Mat과 BufferedImage 간 변환, WebP 지원 등을 제공합니다.
 */
public class ImageUtils {

    private static boolean webpSupportInitialized = false;

    /**
     * WebP 이미지 지원을 초기화합니다.
     */
    public static void initializeWebPSupport() {
        if (!webpSupportInitialized) {
            try {
                // WebP ImageIO 플러그인이 사용 가능한지 확인
                Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("webp");
                if (readers.hasNext()) {
                    System.out.println("WebP 이미지 지원이 활성화되었습니다.");
                } else {
                    System.out.println("WebP 이미지 지원을 찾을 수 없습니다. webp-imageio 라이브러리를 확인하세요.");
                }
                webpSupportInitialized = true;
            } catch (Exception e) {
                System.err.println("WebP 지원 초기화 실패: " + e.getMessage());
            }
        }
    }

    /**
     * 이미지 파일을 BufferedImage로 로드합니다.
     */
    public static BufferedImage loadImage(File imageFile) throws IOException {
        if (!imageFile.exists()) {
            throw new IOException("파일이 존재하지 않습니다: " + imageFile.getPath());
        }

        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                throw new IOException("지원되지 않는 이미지 형식이거나 손상된 파일입니다.");
            }
            return image;
        } catch (IOException e) {
            throw new IOException("이미지 로드 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 이미지 파일을 OpenCV Mat으로 로드합니다.
     */
    public static Mat loadImageAsMat(File imageFile) throws IOException {
        // 먼저 OpenCV의 기본 imread 시도
        Mat image = Imgcodecs.imread(imageFile.getAbsolutePath());

        if (!image.empty()) {
            return image;
        }

        // OpenCV가 실패하면 Java ImageIO 사용
        BufferedImage bufferedImage = loadImage(imageFile);
        return bufferedImageToMat(bufferedImage);
    }

    /**
     * BufferedImage를 OpenCV Mat으로 변환합니다.
     */
    public static Mat bufferedImageToMat(BufferedImage bufferedImage) {
        if (bufferedImage == null) {
            return new Mat();
        }

        // BufferedImage를 BGR 형식으로 변환
        BufferedImage convertedImage = new BufferedImage(
                bufferedImage.getWidth(),
                bufferedImage.getHeight(),
                BufferedImage.TYPE_3BYTE_BGR
        );

        Graphics2D g2d = convertedImage.createGraphics();
        g2d.drawImage(bufferedImage, 0, 0, null);
        g2d.dispose();

        // Mat 생성
        byte[] pixels = ((DataBufferByte) convertedImage.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(convertedImage.getHeight(), convertedImage.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, pixels);

        return mat;
    }

    /**
     * OpenCV Mat을 BufferedImage로 변환합니다.
     */
    public static BufferedImage matToBufferedImage(Mat mat) {
        if (mat.empty()) {
            return null;
        }

        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (mat.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }

        int bufferSize = mat.channels() * mat.cols() * mat.rows();
        byte[] buffer = new byte[bufferSize];
        mat.get(0, 0, buffer); // Mat에서 픽셀 데이터 가져오기

        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(buffer, 0, targetPixels, 0, buffer.length);

        return image;
    }

    /**
     * 이미지를 지정된 크기로 리사이즈합니다.
     */
    public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        if (originalImage == null) {
            return null;
        }

        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, originalImage.getType());
        Graphics2D g2d = resizedImage.createGraphics();

        // 고품질 렌더링 설정
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return resizedImage;
    }

    /**
     * 종횡비를 유지하면서 이미지를 리사이즈합니다.
     */
    public static BufferedImage resizeImageKeepAspectRatio(BufferedImage originalImage, int maxWidth, int maxHeight) {
        if (originalImage == null) {
            return null;
        }

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // 스케일 계산
        double scaleX = (double) maxWidth / originalWidth;
        double scaleY = (double) maxHeight / originalHeight;
        double scale = Math.min(scaleX, scaleY);

        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        return resizeImage(originalImage, newWidth, newHeight);
    }

    /**
     * OpenCV Mat 이미지를 리사이즈합니다.
     */
    public static Mat resizeMat(Mat originalMat, Size newSize) {
        if (originalMat.empty()) {
            return new Mat();
        }

        Mat resizedMat = new Mat();
        Imgproc.resize(originalMat, resizedMat, newSize);
        return resizedMat;
    }

    /**
     * 이미지를 정사각형으로 만듭니다 (패딩 추가).
     */
    public static BufferedImage makeSquare(BufferedImage image, Color paddingColor) {
        if (image == null) {
            return null;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int size = Math.max(width, height);

        BufferedImage squareImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = squareImage.createGraphics();

        // 배경색으로 채우기
        g2d.setColor(paddingColor);
        g2d.fillRect(0, 0, size, size);

        // 이미지를 중앙에 그리기
        int x = (size - width) / 2;
        int y = (size - height) / 2;
        g2d.drawImage(image, x, y, null);
        g2d.dispose();

        return squareImage;
    }

    /**
     * OpenCV Mat을 정사각형으로 만듭니다.
     */
    public static Mat makeSquareMat(Mat image, Scalar paddingColor) {
        if (image.empty()) {
            return new Mat();
        }

        int height = image.rows();
        int width = image.cols();
        int size = Math.max(width, height);

        Mat squareMat = Mat.zeros(size, size, image.type());
        squareMat.setTo(paddingColor);

        // 중앙에 원본 이미지 복사
        int offsetX = (size - width) / 2;
        int offsetY = (size - height) / 2;

        Rect roi = new Rect(offsetX, offsetY, width, height);
        image.copyTo(squareMat.submat(roi));

        return squareMat;
    }

    /**
     * 이미지 품질을 확인합니다.
     */
    public static ImageQuality assessImageQuality(BufferedImage image) {
        if (image == null) {
            return new ImageQuality(false, "이미지가 null입니다.");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        // 최소 크기 확인
        if (width < 32 || height < 32) {
            return new ImageQuality(false, "이미지가 너무 작습니다: " + width + "x" + height);
        }

        // 최대 크기 확인
        if (width > 4096 || height > 4096) {
            return new ImageQuality(false, "이미지가 너무 큽니다: " + width + "x" + height);
        }

        // 종횡비 확인
        double aspectRatio = (double) width / height;
        if (aspectRatio > 10.0 || aspectRatio < 0.1) {
            return new ImageQuality(false, "비정상적인 종횡비: " + aspectRatio);
        }

        return new ImageQuality(true, "정상");
    }

    /**
     * 이미지를 그레이스케일로 변환합니다.
     */
    public static BufferedImage toGrayscale(BufferedImage originalImage) {
        if (originalImage == null) {
            return null;
        }

        BufferedImage grayscaleImage = new BufferedImage(
                originalImage.getWidth(),
                originalImage.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );

        Graphics2D g2d = grayscaleImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, null);
        g2d.dispose();

        return grayscaleImage;
    }

    /**
     * 이미지의 히스토그램을 계산합니다.
     */
    public static int[] calculateHistogram(BufferedImage image, int channel) {
        if (image == null) {
            return new int[256];
        }

        int[] histogram = new int[256];
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int value;

                switch (channel) {
                    case 0: // Red
                        value = (rgb >> 16) & 0xFF;
                        break;
                    case 1: // Green
                        value = (rgb >> 8) & 0xFF;
                        break;
                    case 2: // Blue
                        value = rgb & 0xFF;
                        break;
                    default: // Grayscale
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;
                        value = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                        break;
                }

                histogram[value]++;
            }
        }

        return histogram;
    }

    /**
     * 이미지를 저장합니다.
     */
    public static void saveImage(BufferedImage image, File outputFile, String format) throws IOException {
        if (image == null) {
            throw new IOException("저장할 이미지가 null입니다.");
        }

        boolean success = ImageIO.write(image, format, outputFile);
        if (!success) {
            throw new IOException("이미지 저장 실패: " + outputFile.getPath());
        }
    }

    /**
     * Mat 이미지를 파일로 저장합니다.
     */
    public static void saveMatAsImage(Mat mat, File outputFile) throws IOException {
        if (mat.empty()) {
            throw new IOException("저장할 Mat이 비어있습니다.");
        }

        boolean success = Imgcodecs.imwrite(outputFile.getAbsolutePath(), mat);
        if (!success) {
            throw new IOException("Mat 이미지 저장 실패: " + outputFile.getPath());
        }
    }

    /**
     * 이미지 메타데이터를 추출합니다.
     */
    public static ImageMetadata extractMetadata(File imageFile) {
        ImageMetadata metadata = new ImageMetadata();

        try (ImageInputStream iis = ImageIO.createImageInputStream(imageFile)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(iis);

                metadata.width = reader.getWidth(0);
                metadata.height = reader.getHeight(0);
                metadata.formatName = reader.getFormatName();

                // 추가 메타데이터 추출 (필요시)
                reader.dispose();
            }

        } catch (IOException e) {
            System.err.println("메타데이터 추출 실패: " + e.getMessage());
        }

        return metadata;
    }

    /**
     * 지원되는 이미지 형식인지 확인합니다.
     */
    public static boolean isSupportedImageFormat(File file) {
        if (file == null || !file.exists()) {
            return false;
        }

        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                name.endsWith(".png") || name.endsWith(".webp") ||
                name.endsWith(".bmp") || name.endsWith(".gif") ||
                name.endsWith(".tiff") || name.endsWith(".tif");
    }

    /**
     * 이미지 품질 평가 결과 클래스
     */
    public static class ImageQuality {
        private final boolean isGood;
        private final String message;

        public ImageQuality(boolean isGood, String message) {
            this.isGood = isGood;
            this.message = message;
        }

        public boolean isGood() { return isGood; }
        public String getMessage() { return message; }
    }

    /**
     * 이미지 메타데이터 클래스
     */
    public static class ImageMetadata {
        public int width = 0;
        public int height = 0;
        public String formatName = "";
        public String colorSpace = "";
        public int bitDepth = 0;

        @Override
        public String toString() {
            return String.format("ImageMetadata{%dx%d, format='%s'}", width, height, formatName);
        }
    }
}