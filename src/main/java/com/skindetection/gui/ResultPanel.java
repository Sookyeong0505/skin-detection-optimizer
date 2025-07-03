package com.skindetection.gui;

import com.skindetection.model.AnalysisResult;
import com.skindetection.model.DetectionObject;
import com.skindetection.model.SkinAnalysisData;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * 분석 결과를 표시하는 패널
 * 이미지, YOLO 탐지 결과, 살색 분석 결과를 시각적으로 보여줍니다.
 */
public class ResultPanel extends JPanel {

    // GUI 컴포넌트
    private JLabel imageDisplayLabel;
    private JTextArea yoloResultTextArea;
    private JTextArea skinResultTextArea;
    private JTextArea summaryTextArea;
    private JProgressBar skinRatioProgressBar;
    private JProgressBar crConcentrationProgressBar;
    private JProgressBar cbConcentrationProgressBar;
    private JLabel confidenceLabel;
    private JLabel riskLevelLabel;
    private JLabel recommendationLabel;

    // 현재 표시 중인 결과
    private AnalysisResult currentResult;
    private BufferedImage originalImage;
    private BufferedImage annotatedImage;

    // 설정
    private static final int IMAGE_DISPLAY_SIZE = 400;
    private static final Color HIGH_RISK_COLOR = new Color(255, 0, 0);
    private static final Color MEDIUM_RISK_COLOR = new Color(255, 165, 0);
    private static final Color LOW_RISK_COLOR = new Color(0, 255, 0);

    public ResultPanel() {
        initializeComponents();
        setupLayout();
        clear();
    }

    /**
     * GUI 컴포넌트들을 초기화합니다.
     */
    private void initializeComponents() {
        // 이미지 표시 라벨
        imageDisplayLabel = new JLabel();
        imageDisplayLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageDisplayLabel.setVerticalAlignment(SwingConstants.CENTER);
        imageDisplayLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        imageDisplayLabel.setPreferredSize(new Dimension(IMAGE_DISPLAY_SIZE, IMAGE_DISPLAY_SIZE));

        // YOLO 결과 텍스트 영역
        yoloResultTextArea = new JTextArea(8, 30);
        yoloResultTextArea.setEditable(false);
        yoloResultTextArea.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        yoloResultTextArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 살색 분석 결과 텍스트 영역
        skinResultTextArea = new JTextArea(8, 30);
        skinResultTextArea.setEditable(false);
        skinResultTextArea.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        skinResultTextArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 요약 텍스트 영역
        summaryTextArea = new JTextArea(6, 30);
        summaryTextArea.setEditable(false);
        summaryTextArea.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        summaryTextArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        summaryTextArea.setBackground(new Color(248, 248, 248));

        // 프로그레스 바들
        skinRatioProgressBar = new JProgressBar(0, 100);
        skinRatioProgressBar.setStringPainted(true);
        skinRatioProgressBar.setString("살색 비율: 0%");

        crConcentrationProgressBar = new JProgressBar(0, 100);
        crConcentrationProgressBar.setStringPainted(true);
        crConcentrationProgressBar.setString("Cr 집중도: 0%");

        cbConcentrationProgressBar = new JProgressBar(0, 100);
        cbConcentrationProgressBar.setStringPainted(true);
        cbConcentrationProgressBar.setString("Cb 집중도: 0%");

        // 상태 라벨들
        confidenceLabel = new JLabel("신뢰도: -");
        confidenceLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));

        riskLevelLabel = new JLabel("위험도: -");
        riskLevelLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));

        recommendationLabel = new JLabel("권장사항: -");
        recommendationLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
    }

    /**
     * GUI 레이아웃을 설정합니다.
     */
    private void setupLayout() {
        setLayout(new BorderLayout());

        // 좌측: 이미지 표시
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setBorder(new TitledBorder("원본 이미지"));
        imagePanel.add(imageDisplayLabel, BorderLayout.CENTER);

        // 우측: 분석 결과
        JPanel resultPanel = createResultPanel();

        // 하단: 요약 정보
        JPanel summaryPanel = createSummaryPanel();

        // 전체 레이아웃
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                imagePanel, resultPanel);
        mainSplitPane.setDividerLocation(IMAGE_DISPLAY_SIZE + 20);
        mainSplitPane.setResizeWeight(0.4);

        add(mainSplitPane, BorderLayout.CENTER);
        add(summaryPanel, BorderLayout.SOUTH);
    }

    /**
     * 분석 결과 패널을 생성합니다.
     */
    private JPanel createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 탭 패널 생성
        JTabbedPane tabbedPane = new JTabbedPane();

        // YOLO 결과 탭
        JScrollPane yoloScrollPane = new JScrollPane(yoloResultTextArea);
        yoloScrollPane.setBorder(new TitledBorder("YOLO 객체 탐지 결과"));
        tabbedPane.addTab("객체 탐지", yoloScrollPane);

        // 살색 분석 결과 탭
        JPanel skinPanel = createSkinAnalysisPanel();
        tabbedPane.addTab("살색 분석", skinPanel);

        panel.add(tabbedPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 살색 분석 패널을 생성합니다.
     */
    private JPanel createSkinAnalysisPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 상단: 프로그레스 바들
        JPanel progressPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        progressPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        progressPanel.add(skinRatioProgressBar);
        progressPanel.add(crConcentrationProgressBar);
        progressPanel.add(cbConcentrationProgressBar);

        // 중앙: 상세 텍스트
        JScrollPane skinScrollPane = new JScrollPane(skinResultTextArea);

        panel.add(progressPanel, BorderLayout.NORTH);
        panel.add(skinScrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 요약 패널을 생성합니다.
     */
    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("종합 평가"));

        // 좌측: 상태 정보
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(confidenceLabel);
        statusPanel.add(Box.createHorizontalStrut(20));
        statusPanel.add(riskLevelLabel);
        statusPanel.add(Box.createHorizontalStrut(20));
        statusPanel.add(recommendationLabel);

        // 우측: 요약 텍스트
        JScrollPane summaryScrollPane = new JScrollPane(summaryTextArea);
        summaryScrollPane.setPreferredSize(new Dimension(300, 120));

        panel.add(statusPanel, BorderLayout.NORTH);
        panel.add(summaryScrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 분석 결과를 표시합니다.
     */
    public void displayResult(AnalysisResult result) {
        this.currentResult = result;

        if (result == null) {
            clear();
            return;
        }

        // 이미지 표시
        displayImage(result);

        // YOLO 결과 표시
        displayYoloResults(result);

        // 살색 분석 결과 표시
        displaySkinAnalysisResults(result);

        // 요약 정보 표시
        displaySummary(result);

        // 화면 새로고침
        repaint();
    }

    /**
     * 이미지를 표시합니다.
     */
    private void displayImage(AnalysisResult result) {
        try {
            File imageFile = result.getImageData().getFile();
            BufferedImage image = javax.imageio.ImageIO.read(imageFile);

            if (image != null) {
                this.originalImage = image;

                // 탐지 결과를 오버레이한 이미지 생성
                this.annotatedImage = createAnnotatedImage(image, result.getDetectedObjects());

                // 썸네일 크기로 조정
                BufferedImage thumbnail = resizeImage(annotatedImage, IMAGE_DISPLAY_SIZE);
                imageDisplayLabel.setIcon(new ImageIcon(thumbnail));
                imageDisplayLabel.setText("");
            }

        } catch (Exception e) {
            imageDisplayLabel.setIcon(null);
            imageDisplayLabel.setText("이미지 로드 실패: " + e.getMessage());
        }
    }

    /**
     * 탐지 결과가 오버레이된 이미지를 생성합니다.
     */
    private BufferedImage createAnnotatedImage(BufferedImage original, List<DetectionObject> detections) {
        BufferedImage annotated = new BufferedImage(
                original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = annotated.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 원본 이미지 그리기
        g2d.drawImage(original, 0, 0, null);

        // 탐지 결과 오버레이
        if (detections != null && !detections.isEmpty()) {
            g2d.setStroke(new BasicStroke(3.0f));

            for (DetectionObject detection : detections) {
                float[] bbox = detection.getBoundingBox();
                Color color = detection.getClassColor();

                // 바운딩 박스 그리기
                g2d.setColor(color);
                g2d.drawRect((int)bbox[0], (int)bbox[1],
                        (int)(bbox[2] - bbox[0]), (int)(bbox[3] - bbox[1]));

                // 라벨 그리기
                String label = detection.getDisplayText();
                FontMetrics fm = g2d.getFontMetrics();
                int labelWidth = fm.stringWidth(label);
                int labelHeight = fm.getHeight();

                // 라벨 배경
                g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 180));
                g2d.fillRect((int)bbox[0], (int)bbox[1] - labelHeight,
                        labelWidth + 10, labelHeight);

                // 라벨 텍스트
                g2d.setColor(Color.WHITE);
                g2d.drawString(label, (int)bbox[0] + 5, (int)bbox[1] - 5);
            }
        }

        g2d.dispose();
        return annotated;
    }

    /**
     * 이미지 크기를 조정합니다.
     */
    private BufferedImage resizeImage(BufferedImage original, int maxSize) {
        int width = original.getWidth();
        int height = original.getHeight();

        double scale = Math.min((double) maxSize / width, (double) maxSize / height);
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return resized;
    }

    /**
     * YOLO 탐지 결과를 표시합니다.
     */
    private void displayYoloResults(AnalysisResult result) {
        StringBuilder yoloText = new StringBuilder();

        yoloText.append("=== YOLO 객체 탐지 결과 ===\n\n");
        yoloText.append("모델: ").append(result.getModelName()).append("\n");
        yoloText.append("탐지된 객체 수: ").append(result.getDetectedObjectsCount()).append("\n\n");

        if (result.getDetectedObjects() != null && !result.getDetectedObjects().isEmpty()) {
            yoloText.append("탐지된 객체 목록:\n");

            int index = 1;
            for (DetectionObject detection : result.getDetectedObjects()) {
                yoloText.append(String.format("%d. %s\n", index++, detection.getDetailedInfo()));
                yoloText.append("\n");
            }

            DetectionObject highest = result.getHighestConfidenceDetection();
            if (highest != null) {
                yoloText.append("최고 신뢰도 탐지:\n");
                yoloText.append(highest.getDetailedInfo()).append("\n\n");
            }
        } else {
            yoloText.append("탐지된 객체가 없습니다.\n\n");
        }

        // 클래스별 신뢰도
        yoloText.append("클래스별 최고 신뢰도:\n");
        yoloText.append(String.format("여성 가슴: %.1f%%\n", result.getExposedBreastFConfidence() * 100));
        yoloText.append(String.format("남성 가슴: %.1f%%\n", result.getExposedBreastMConfidence() * 100));
        yoloText.append(String.format("엉덩이: %.1f%%\n", result.getExposedButtocksConfidence() * 100));
        yoloText.append(String.format("여성 성기: %.1f%%\n", result.getExposedGenitaliaFConfidence() * 100));
        yoloText.append(String.format("남성 성기: %.1f%%\n", result.getExposedGenitaliaMConfidence() * 100));

        yoloResultTextArea.setText(yoloText.toString());
        yoloResultTextArea.setCaretPosition(0);
    }

    /**
     * 살색 분석 결과를 표시합니다.
     */
    private void displaySkinAnalysisResults(AnalysisResult result) {
        SkinAnalysisData skinData = result.getSkinAnalysisData();

        if (skinData == null) {
            skinResultTextArea.setText("살색 분석 데이터가 없습니다.");
            resetProgressBars();
            return;
        }

        // 프로그레스 바 업데이트
        updateProgressBar(skinRatioProgressBar, skinData.getTotalSkinRatio(),
                "살색 비율: %.1f%%");
        updateProgressBar(crConcentrationProgressBar, skinData.getCrChannelConcentration(),
                "Cr 집중도: %.1f%%");
        updateProgressBar(cbConcentrationProgressBar, skinData.getCbChannelConcentration(),
                "Cb 집중도: %.1f%%");

        // 상세 텍스트 표시
        skinResultTextArea.setText(skinData.getDetailedAnalysisReport());
        skinResultTextArea.setCaretPosition(0);
    }

    /**
     * 프로그레스 바를 업데이트합니다.
     */
    private void updateProgressBar(JProgressBar progressBar, double value, String format) {
        int intValue = (int) Math.round(value);
        progressBar.setValue(intValue);
        progressBar.setString(String.format(format, value));

        // 색상 설정
        if (value >= 40) {
            progressBar.setForeground(HIGH_RISK_COLOR);
        } else if (value >= 25) {
            progressBar.setForeground(MEDIUM_RISK_COLOR);
        } else {
            progressBar.setForeground(LOW_RISK_COLOR);
        }
    }

    /**
     * 프로그레스 바들을 초기화합니다.
     */
    private void resetProgressBars() {
        skinRatioProgressBar.setValue(0);
        skinRatioProgressBar.setString("살색 비율: 0%");
        crConcentrationProgressBar.setValue(0);
        crConcentrationProgressBar.setString("Cr 집중도: 0%");
        cbConcentrationProgressBar.setValue(0);
        cbConcentrationProgressBar.setString("Cb 집중도: 0%");
    }

    /**
     * 요약 정보를 표시합니다.
     */
    private void displaySummary(AnalysisResult result) {
        // 상태 라벨 업데이트
        confidenceLabel.setText(String.format("신뢰도: %.3f", result.getSkinConfidenceCombined()));

        riskLevelLabel.setText("위험도: " + result.getRiskLevel());
        riskLevelLabel.setForeground(getRiskLevelColor(result.getRiskLevel()));

        recommendationLabel.setText("권장사항: " + result.getFilterRecommendation());
        recommendationLabel.setForeground(getRecommendationColor(result.getFilterRecommendation()));

        // 요약 텍스트
        summaryTextArea.setText(result.getSummary());
        summaryTextArea.setCaretPosition(0);
    }

    /**
     * 위험도에 따른 색상을 반환합니다.
     */
    private Color getRiskLevelColor(String riskLevel) {
        switch (riskLevel) {
            case "HIGH": return HIGH_RISK_COLOR;
            case "MEDIUM": return MEDIUM_RISK_COLOR;
            case "LOW": return LOW_RISK_COLOR;
            default: return Color.BLACK;
        }
    }

    /**
     * 권장사항에 따른 색상을 반환합니다.
     */
    private Color getRecommendationColor(String recommendation) {
        switch (recommendation) {
            case "BLOCK": return HIGH_RISK_COLOR;
            case "REVIEW": return MEDIUM_RISK_COLOR;
            case "ALLOW": return LOW_RISK_COLOR;
            default: return Color.BLACK;
        }
    }

    /**
     * 모든 표시 내용을 초기화합니다.
     */
    public void clear() {
        imageDisplayLabel.setIcon(null);
        imageDisplayLabel.setText("이미지가 선택되지 않았습니다.");

        yoloResultTextArea.setText("분석 결과가 없습니다.");
        skinResultTextArea.setText("분석 결과가 없습니다.");
        summaryTextArea.setText("분석 결과가 없습니다.");

        resetProgressBars();

        confidenceLabel.setText("신뢰도: -");
        riskLevelLabel.setText("위험도: -");
        riskLevelLabel.setForeground(Color.BLACK);
        recommendationLabel.setText("권장사항: -");
        recommendationLabel.setForeground(Color.BLACK);

        currentResult = null;
        originalImage = null;
        annotatedImage = null;
    }

    /**
     * 현재 표시 중인 결과를 반환합니다.
     */
    public AnalysisResult getCurrentResult() {
        return currentResult;
    }

    /**
     * 원본 이미지를 반환합니다.
     */
    public BufferedImage getOriginalImage() {
        return originalImage;
    }

    /**
     * 어노테이션된 이미지를 반환합니다.
     */
    public BufferedImage getAnnotatedImage() {
        return annotatedImage;
    }
}