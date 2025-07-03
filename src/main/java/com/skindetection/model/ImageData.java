package com.skindetection.model;

import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 이미지 파일의 메타데이터를 담는 데이터 모델 클래스
 */
public class ImageData {

    private File file;
    private String fileName;
    private String filePath;
    private long fileSizeBytes;
    private double fileSizeKB;
    private double fileSizeMB;
    private String fileExtension;
    private LocalDateTime creationTime;
    private LocalDateTime lastModifiedTime;

    // 이미지 속성
    private int width;
    private int height;
    private int totalPixels;
    private String colorSpace;
    private int bitDepth;
    private boolean hasAlphaChannel;

    // 추가 메타데이터
    private String mimeType;
    private boolean isSupported;
    private String errorMessage;

    /**
     * 생성자
     */
    public ImageData(File file) {
        this.file = file;
        initializeFromFile();
    }

    /**
     * 파일로부터 기본 정보를 초기화합니다.
     */
    private void initializeFromFile() {
        if (file == null || !file.exists()) {
            this.isSupported = false;
            this.errorMessage = "파일이 존재하지 않습니다.";
            return;
        }

        try {
            // 기본 파일 정보
            this.fileName = file.getName();
            this.filePath = file.getAbsolutePath();
            this.fileSizeBytes = file.length();
            this.fileSizeKB = fileSizeBytes / 1024.0;
            this.fileSizeMB = fileSizeKB / 1024.0;

            // 파일 확장자
            int lastDot = fileName.lastIndexOf('.');
            this.fileExtension = lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";

            // 파일 시간 정보
            this.lastModifiedTime = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(file.lastModified()),
                    ZoneId.systemDefault()
            );

            // 생성 시간 (가능한 경우)
            try {
                this.creationTime = LocalDateTime.ofInstant(
                        Files.readAttributes(file.toPath(), java.nio.file.attribute.BasicFileAttributes.class)
                                .creationTime().toInstant(),
                        ZoneId.systemDefault()
                );
            } catch (Exception e) {
                this.creationTime = this.lastModifiedTime;
            }

            // MIME 타입 추정
            this.mimeType = determineMimeType();

            // 지원 여부 확인
            this.isSupported = isSupportedFormat();

            if (!isSupported) {
                this.errorMessage = "지원되지 않는 이미지 형식입니다: " + fileExtension;
            }

        } catch (Exception e) {
            this.isSupported = false;
            this.errorMessage = "파일 정보 읽기 실패: " + e.getMessage();
        }
    }

    /**
     * 이미지 차원 정보를 설정합니다.
     */
    public void setImageDimensions(int width, int height) {
        this.width = width;
        this.height = height;
        this.totalPixels = width * height;
    }

    /**
     * 이미지 속성을 설정합니다.
     */
    public void setImageProperties(String colorSpace, int bitDepth, boolean hasAlphaChannel) {
        this.colorSpace = colorSpace;
        this.bitDepth = bitDepth;
        this.hasAlphaChannel = hasAlphaChannel;
    }

    /**
     * MIME 타입을 결정합니다.
     */
    private String determineMimeType() {
        switch (fileExtension.toLowerCase()) {
            case "webp":
                return "image/webp";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "tiff":
            case "tif":
                return "image/tiff";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * 지원되는 이미지 형식인지 확인합니다.
     */
    private boolean isSupportedFormat() {
        return fileExtension.matches("(?i)webp|jpg|jpeg|png|gif|bmp|tiff?");
    }

    /**
     * 이미지 크기 비율을 반환합니다.
     */
    public double getAspectRatio() {
        if (height == 0) return 0.0;
        return (double) width / height;
    }

    /**
     * 이미지가 가로형인지 확인합니다.
     */
    public boolean isLandscape() {
        return width > height;
    }

    /**
     * 이미지가 세로형인지 확인합니다.
     */
    public boolean isPortrait() {
        return height > width;
    }

    /**
     * 이미지가 정사각형인지 확인합니다.
     */
    public boolean isSquare() {
        return width == height;
    }

    /**
     * 적절한 썸네일 크기를 계산합니다.
     */
    public Dimension calculateThumbnailSize(int maxSize) {
        if (width == 0 || height == 0) {
            return new Dimension(maxSize, maxSize);
        }

        double scale = Math.min((double) maxSize / width, (double) maxSize / height);
        int thumbWidth = (int) (width * scale);
        int thumbHeight = (int) (height * scale);

        return new Dimension(thumbWidth, thumbHeight);
    }

    /**
     * 파일 크기를 적절한 단위로 포맷팅합니다.
     */
    public String getFormattedFileSize() {
        if (fileSizeMB >= 1.0) {
            return String.format("%.1f MB", fileSizeMB);
        } else if (fileSizeKB >= 1.0) {
            return String.format("%.1f KB", fileSizeKB);
        } else {
            return String.format("%d bytes", fileSizeBytes);
        }
    }

    /**
     * 이미지 해상도 문자열을 반환합니다.
     */
    public String getResolutionString() {
        if (width > 0 && height > 0) {
            return String.format("%d x %d", width, height);
        }
        return "Unknown";
    }

    /**
     * 상세 정보를 문자열로 반환합니다.
     */
    public String getDetailedInfo() {
        StringBuilder info = new StringBuilder();

        info.append("=== 이미지 정보 ===\n");
        info.append("파일명: ").append(fileName).append("\n");
        info.append("경로: ").append(filePath).append("\n");
        info.append("크기: ").append(getFormattedFileSize()).append("\n");
        info.append("형식: ").append(fileExtension.toUpperCase()).append("\n");
        info.append("MIME 타입: ").append(mimeType).append("\n");

        if (width > 0 && height > 0) {
            info.append("해상도: ").append(getResolutionString()).append("\n");
            info.append("종횡비: ").append(String.format("%.2f", getAspectRatio())).append("\n");
            info.append("총 픽셀: ").append(String.format("%,d", totalPixels)).append("\n");
        }

        if (colorSpace != null) {
            info.append("색공간: ").append(colorSpace).append("\n");
            info.append("비트 깊이: ").append(bitDepth).append("\n");
            info.append("알파 채널: ").append(hasAlphaChannel ? "있음" : "없음").append("\n");
        }

        info.append("지원 여부: ").append(isSupported ? "지원됨" : "지원 안됨").append("\n");
        if (errorMessage != null) {
            info.append("오류: ").append(errorMessage).append("\n");
        }

        info.append("수정 시간: ").append(lastModifiedTime).append("\n");

        return info.toString();
    }

    /**
     * 요약 정보를 반환합니다.
     */
    public String getSummary() {
        if (!isSupported) {
            return fileName + " (지원 안됨)";
        }

        String sizeInfo = width > 0 ? String.format(" - %dx%d", width, height) : "";
        return fileName + sizeInfo + " (" + getFormattedFileSize() + ")";
    }

    // Getter/Setter 메서드들

    public File getFile() {
        return file;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public double getFileSizeKB() {
        return fileSizeKB;
    }

    public double getFileSizeMB() {
        return fileSizeMB;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public LocalDateTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
        this.totalPixels = width * height;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
        this.totalPixels = width * height;
    }

    public int getTotalPixels() {
        return totalPixels;
    }

    public String getColorSpace() {
        return colorSpace;
    }

    public void setColorSpace(String colorSpace) {
        this.colorSpace = colorSpace;
    }

    public int getBitDepth() {
        return bitDepth;
    }

    public void setBitDepth(int bitDepth) {
        this.bitDepth = bitDepth;
    }

    public boolean isHasAlphaChannel() {
        return hasAlphaChannel;
    }

    public void setHasAlphaChannel(boolean hasAlphaChannel) {
        this.hasAlphaChannel = hasAlphaChannel;
    }

    public String getMimeType() {
        return mimeType;
    }

    public boolean isSupported() {
        return isSupported;
    }

    public void setSupported(boolean supported) {
        isSupported = supported;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return getSummary();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ImageData imageData = (ImageData) obj;
        return file != null ? file.equals(imageData.file) : imageData.file == null;
    }

    @Override
    public int hashCode() {
        return file != null ? file.hashCode() : 0;
    }
}