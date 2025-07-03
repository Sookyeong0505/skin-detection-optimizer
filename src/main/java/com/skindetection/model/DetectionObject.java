package com.skindetection.model;

import java.awt.*;

/**
 * YOLO 모델의 객체 탐지 결과를 담는 데이터 모델 클래스
 */
public class DetectionObject {

    private int classId;
    private String className;
    private float confidence;
    private float[] boundingBox; // [x1, y1, x2, y2]
    private Rectangle boundingRect;
    private double area;

    /**
     * 생성자
     */
    public DetectionObject(int classId, String className, float confidence, float[] boundingBox) {
        this.classId = classId;
        this.className = className;
        this.confidence = confidence;
        this.boundingBox = boundingBox.clone();

        // Rectangle 객체 생성
        this.boundingRect = new Rectangle(
                (int) boundingBox[0],
                (int) boundingBox[1],
                (int) (boundingBox[2] - boundingBox[0]),
                (int) (boundingBox[3] - boundingBox[1])
        );

        // 면적 계산
        this.area = (boundingBox[2] - boundingBox[0]) * (boundingBox[3] - boundingBox[1]);
    }

    /**
     * 바운딩 박스의 중심점을 반환합니다.
     */
    public Point getCenterPoint() {
        int centerX = (int) ((boundingBox[0] + boundingBox[2]) / 2);
        int centerY = (int) ((boundingBox[1] + boundingBox[3]) / 2);
        return new Point(centerX, centerY);
    }

    /**
     * 바운딩 박스의 너비를 반환합니다.
     */
    public float getWidth() {
        return boundingBox[2] - boundingBox[0];
    }

    /**
     * 바운딩 박스의 높이를 반환합니다.
     */
    public float getHeight() {
        return boundingBox[3] - boundingBox[1];
    }

    /**
     * 신뢰도를 백분율로 반환합니다.
     */
    public double getConfidencePercent() {
        return confidence * 100.0;
    }

    /**
     * 다른 DetectionObject와의 IoU(Intersection over Union)를 계산합니다.
     */
    public double calculateIoU(DetectionObject other) {
        float[] box1 = this.boundingBox;
        float[] box2 = other.boundingBox;

        float x1 = Math.max(box1[0], box2[0]);
        float y1 = Math.max(box1[1], box2[1]);
        float x2 = Math.min(box1[2], box2[2]);
        float y2 = Math.min(box1[3], box2[3]);

        if (x2 <= x1 || y2 <= y1) {
            return 0.0;
        }

        float intersection = (x2 - x1) * (y2 - y1);
        float union = (float) (this.area + other.area - intersection);

        return intersection / union;
    }

    /**
     * 객체가 이미지 경계 내에 있는지 확인합니다.
     */
    public boolean isWithinImageBounds(int imageWidth, int imageHeight) {
        return boundingBox[0] >= 0 && boundingBox[1] >= 0 &&
                boundingBox[2] <= imageWidth && boundingBox[3] <= imageHeight;
    }

    /**
     * 바운딩 박스를 스케일링합니다.
     */
    public DetectionObject scale(double scaleX, double scaleY) {
        float[] scaledBox = new float[4];
        scaledBox[0] = (float) (boundingBox[0] * scaleX);
        scaledBox[1] = (float) (boundingBox[1] * scaleY);
        scaledBox[2] = (float) (boundingBox[2] * scaleX);
        scaledBox[3] = (float) (boundingBox[3] * scaleY);

        return new DetectionObject(classId, className, confidence, scaledBox);
    }

    /**
     * 클래스에 따른 색상을 반환합니다.
     */
    public Color getClassColor() {
        switch (className) {
            case "EXPOSED_BREAST_F":
                return new Color(255, 0, 0); // 빨간색
            case "EXPOSED_BREAST_M":
                return new Color(255, 128, 0); // 주황색
            case "EXPOSED_BUTTOCKS":
                return new Color(255, 255, 0); // 노란색
            case "EXPOSED_GENITALIA_F":
                return new Color(255, 0, 255); // 자홍색
            case "EXPOSED_GENITALIA_M":
                return new Color(128, 0, 255); // 보라색
            default:
                return new Color(0, 255, 0); // 기본 녹색
        }
    }

    /**
     * 탐지 정보를 문자열로 반환합니다.
     */
    public String getDisplayText() {
        return String.format("%s (%.1f%%)", className, getConfidencePercent());
    }

    /**
     * 상세 정보를 문자열로 반환합니다.
     */
    public String getDetailedInfo() {
        return String.format(
                "클래스: %s (ID: %d)\n신뢰도: %.1f%%\n" +
                        "위치: (%.0f, %.0f) - (%.0f, %.0f)\n" +
                        "크기: %.0f x %.0f (면적: %.0f)",
                className, classId, getConfidencePercent(),
                boundingBox[0], boundingBox[1], boundingBox[2], boundingBox[3],
                getWidth(), getHeight(), area
        );
    }

    // Getter/Setter 메서드들

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public float[] getBoundingBox() {
        return boundingBox.clone();
    }

    public void setBoundingBox(float[] boundingBox) {
        this.boundingBox = boundingBox.clone();

        // Rectangle 및 area 재계산
        this.boundingRect = new Rectangle(
                (int) boundingBox[0],
                (int) boundingBox[1],
                (int) (boundingBox[2] - boundingBox[0]),
                (int) (boundingBox[3] - boundingBox[1])
        );
        this.area = (boundingBox[2] - boundingBox[0]) * (boundingBox[3] - boundingBox[1]);
    }

    public Rectangle getBoundingRect() {
        return new Rectangle(boundingRect);
    }

    public double getArea() {
        return area;
    }

    @Override
    public String toString() {
        return String.format("DetectionObject{class='%s', confidence=%.3f, bbox=[%.1f,%.1f,%.1f,%.1f]}",
                className, confidence, boundingBox[0], boundingBox[1], boundingBox[2], boundingBox[3]);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        DetectionObject that = (DetectionObject) obj;
        return classId == that.classId &&
                Float.compare(that.confidence, confidence) == 0 &&
                className.equals(that.className) &&
                java.util.Arrays.equals(boundingBox, that.boundingBox);
    }

    @Override
    public int hashCode() {
        int result = java.util.Objects.hash(classId, className, confidence);
        result = 31 * result + java.util.Arrays.hashCode(boundingBox);
        return result;
    }
}