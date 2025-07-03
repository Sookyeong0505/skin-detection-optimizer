package com.skindetection.gui;

import com.skindetection.analyzer.YoloAnalyzer;
import com.skindetection.analyzer.SkinColorAnalyzer;
import com.skindetection.data.DataCollector;
import com.skindetection.data.ExcelExporter;
import com.skindetection.model.AnalysisResult;
import com.skindetection.model.ImageData;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 메인 GUI 프레임
 * 선정성 탐지 분석 및 데이터 수집을 위한 사용자 인터페이스
 */
public class MainFrame extends JFrame {

    // GUI 컴포넌트
    private JComboBox<String> modelComboBox;
    private JButton selectImagesButton;
    private JList<ImageData> imageList;
    private DefaultListModel<ImageData> imageListModel;
    private JRadioButton truePositiveRadio;
    private JRadioButton falsePositiveRadio;
    private ButtonGroup classificationGroup;
    private JTextArea featuresTextArea;
    private JButton analyzeButton;
    private ResultPanel resultPanel;
    private JButton saveButton;
    private JButton exportExcelButton;
    private JProgressBar progressBar;
    private JLabel statusLabel;

    // 데이터 관리
    private DataCollector dataCollector;
    private YoloAnalyzer yoloAnalyzer;
    private SkinColorAnalyzer skinAnalyzer;
    private List<AnalysisResult> analysisResults;

    public MainFrame() {
        initializeComponents();
        setupLayout();
        setupEventListeners();
        initializeAnalyzers();

        setTitle("선정성 탐지 최적화 프로그램 v1.0");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    /**
     * GUI 컴포넌트들을 초기화합니다.
     */
    private void initializeComponents() {
        // 모델 선택
        modelComboBox = new JComboBox<>(new String[]{
                "best_640n_0522.onnx",
                "yolov8n.onnx",
                "yolov8s.onnx"
        });

        // 이미지 선택
        selectImagesButton = new JButton("이미지 선택");
        selectImagesButton.setFont(new Font("맑은 고딕", Font.BOLD, 12));

        // 이미지 리스트
        imageListModel = new DefaultListModel<>();
        imageList = new JList<>(imageListModel);
        imageList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        imageList.setCellRenderer(new ImageListCellRenderer());

        // 분류 라디오 버튼
        truePositiveRadio = new JRadioButton("정탐 (True Positive)", true);
        falsePositiveRadio = new JRadioButton("오탐 (False Positive)");
        classificationGroup = new ButtonGroup();
        classificationGroup.add(truePositiveRadio);
        classificationGroup.add(falsePositiveRadio);

        // 특징 입력 영역
        featuresTextArea = new JTextArea(3, 20);
        featuresTextArea.setLineWrap(true);
        featuresTextArea.setWrapStyleWord(true);
        featuresTextArea.setBorder(BorderFactory.createTitledBorder("오탐 특징 설명"));

        // 분석 버튼
        analyzeButton = new JButton("분석 시작");
        analyzeButton.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        analyzeButton.setBackground(new Color(52, 152, 219));
        analyzeButton.setForeground(Color.WHITE);

        // 결과 패널
        resultPanel = new ResultPanel();

        // 저장/내보내기 버튼
        saveButton = new JButton("결과 저장");
        exportExcelButton = new JButton("엑셀로 다운로드");

        // 상태바
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        statusLabel = new JLabel("프로그램을 시작했습니다.");

        // 초기 상태 설정
        analyzeButton.setEnabled(false);
        saveButton.setEnabled(false);
        featuresTextArea.setEnabled(false);
    }

    /**
     * GUI 레이아웃을 설정합니다.
     */
    private void setupLayout() {
        setLayout(new BorderLayout());

        // 상단 컨트롤 패널
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // 중앙 메인 패널
        JPanel centerPanel = createCenterPanel();
        add(centerPanel, BorderLayout.CENTER);

        // 하단 상태바
        JPanel bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * 상단 컨트롤 패널을 생성합니다.
     */
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(new TitledBorder("설정"));

        panel.add(new JLabel("모델:"));
        panel.add(modelComboBox);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(selectImagesButton);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(analyzeButton);

        return panel;
    }

    /**
     * 중앙 메인 패널을 생성합니다.
     */
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 좌측 패널: 이미지 목록 및 분류
        JPanel leftPanel = createLeftPanel();

        // 우측 패널: 결과 표시
        JPanel rightPanel = createRightPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(400);
        splitPane.setResizeWeight(0.3);

        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 좌측 패널을 생성합니다.
     */
    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 이미지 리스트
        JScrollPane imageScrollPane = new JScrollPane(imageList);
        imageScrollPane.setBorder(new TitledBorder("선택된 이미지"));
        imageScrollPane.setPreferredSize(new Dimension(380, 300));

        // 분류 패널
        JPanel classificationPanel = new JPanel(new GridLayout(2, 1));
        classificationPanel.setBorder(new TitledBorder("분류"));
        classificationPanel.add(truePositiveRadio);
        classificationPanel.add(falsePositiveRadio);

        // 특징 입력 패널
        JScrollPane featuresScrollPane = new JScrollPane(featuresTextArea);

        // 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(saveButton);
        buttonPanel.add(exportExcelButton);

        panel.add(imageScrollPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.add(classificationPanel, BorderLayout.NORTH);
        controlPanel.add(featuresScrollPane, BorderLayout.CENTER);
        controlPanel.add(buttonPanel, BorderLayout.SOUTH);

        panel.add(controlPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * 우측 패널을 생성합니다.
     */
    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("분석 결과"));
        panel.add(resultPanel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * 하단 상태바 패널을 생성합니다.
     */
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());

        panel.add(progressBar, BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.EAST);

        return panel;
    }

    /**
     * 이벤트 리스너들을 설정합니다.
     */
    private void setupEventListeners() {
        // 이미지 선택 버튼
        selectImagesButton.addActionListener(e -> selectImages());

        // 분류 라디오 버튼
        falsePositiveRadio.addActionListener(e -> featuresTextArea.setEnabled(true));
        truePositiveRadio.addActionListener(e -> {
            featuresTextArea.setEnabled(false);
            featuresTextArea.setText("");
        });

        // 분석 버튼
        analyzeButton.addActionListener(e -> performAnalysis());

        // 저장 버튼
        saveButton.addActionListener(e -> saveResults());

        // 엑셀 내보내기 버튼
        exportExcelButton.addActionListener(e -> exportToExcel());

        // 이미지 리스트 선택 변경
        imageList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateResultDisplay();
            }
        });
    }

    /**
     * 분석기들을 초기화합니다.
     */
    private void initializeAnalyzers() {
        dataCollector = new DataCollector();
        analysisResults = new ArrayList<>();

        try {
            yoloAnalyzer = new YoloAnalyzer();
            skinAnalyzer = new SkinColorAnalyzer();
            statusLabel.setText("분석기 초기화 완료");
        } catch (Exception e) {
            statusLabel.setText("분석기 초기화 실패: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "분석기 초기화 중 오류가 발생했습니다:\n" + e.getMessage(),
                    "초기화 오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 이미지 파일들을 선택합니다.
     */
    private void selectImages() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "이미지 파일 (*.webp, *.jpg, *.png)", "webp", "jpg", "jpeg", "png"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();

            imageListModel.clear();
            for (File file : selectedFiles) {
                ImageData imageData = new ImageData(file);
                imageListModel.addElement(imageData);
            }

            analyzeButton.setEnabled(imageListModel.getSize() > 0);
            statusLabel.setText(imageListModel.getSize() + "개 이미지가 선택되었습니다.");
        }
    }

    /**
     * 선택된 이미지들에 대해 분석을 수행합니다.
     */
    private void performAnalysis() {
        if (imageListModel.getSize() == 0) {
            JOptionPane.showMessageDialog(this, "분석할 이미지를 선택해주세요.",
                    "이미지 미선택", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 백그라운드에서 분석 실행
        SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                analyzeButton.setEnabled(false);
                progressBar.setMinimum(0);
                progressBar.setMaximum(imageListModel.getSize());

                String selectedModel = (String) modelComboBox.getSelectedItem();

                for (int i = 0; i < imageListModel.getSize(); i++) {
                    ImageData imageData = imageListModel.getElementAt(i);
                    statusLabel.setText("분석 중: " + imageData.getFileName());

                    // 분석 수행
                    AnalysisResult result = performSingleImageAnalysis(imageData, selectedModel);
                    analysisResults.add(result);

                    publish(i + 1);

                    // 취소 확인
                    if (isCancelled()) break;
                }

                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                int latest = chunks.get(chunks.size() - 1);
                progressBar.setValue(latest);
                progressBar.setString(latest + "/" + imageListModel.getSize());
            }

            @Override
            protected void done() {
                analyzeButton.setEnabled(true);
                saveButton.setEnabled(true);
                progressBar.setValue(0);
                progressBar.setString("");
                statusLabel.setText("분석 완료");

                if (!analysisResults.isEmpty()) {
                    imageList.setSelectedIndex(0);
                    updateResultDisplay();
                }
            }
        };

        worker.execute();
    }

    /**
     * 단일 이미지에 대한 분석을 수행합니다.
     */
    private AnalysisResult performSingleImageAnalysis(ImageData imageData, String modelName) {
        try {
            // YOLO 분석
            var yoloResult = yoloAnalyzer.analyze(imageData.getFile(), modelName);

            // 살색 분석
            var skinResult = skinAnalyzer.analyze(imageData.getFile());

            // 결과 통합
            AnalysisResult result = new AnalysisResult(imageData, yoloResult, skinResult);

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("분석 오류: " + e.getMessage());
            return new AnalysisResult(imageData, null, null); // 오류 결과
        }
    }

    /**
     * 결과 화면을 업데이트합니다.
     */
    private void updateResultDisplay() {
        int selectedIndex = imageList.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < analysisResults.size()) {
            AnalysisResult result = analysisResults.get(selectedIndex);
            resultPanel.displayResult(result);
        }
    }

    /**
     * 분석 결과를 저장합니다.
     */
    private void saveResults() {
        if (analysisResults.isEmpty()) {
            JOptionPane.showMessageDialog(this, "저장할 결과가 없습니다.",
                    "결과 없음", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean isPositive = truePositiveRadio.isSelected();
        String features = featuresTextArea.getText().trim();

        if (!isPositive && features.isEmpty()) {
            JOptionPane.showMessageDialog(this, "오탐인 경우 특징을 입력해주세요.",
                    "특징 미입력", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 데이터 컬렉터에 결과 추가
        for (AnalysisResult result : analysisResults) {
            result.setClassification(isPositive ? "정탐" : "오탐");
            result.setFeatures(features);
            dataCollector.addResult(result);
        }

        JOptionPane.showMessageDialog(this,
                analysisResults.size() + "개 결과가 저장되었습니다.\n총 " +
                        dataCollector.getResultCount() + "개 데이터가 수집되었습니다.",
                "저장 완료", JOptionPane.INFORMATION_MESSAGE);

        // 초기화
        analysisResults.clear();
        imageListModel.clear();
        resultPanel.clear();
        analyzeButton.setEnabled(false);
        saveButton.setEnabled(false);
        featuresTextArea.setText("");
        statusLabel.setText("결과 저장 완료");
    }

    /**
     * 수집된 데이터를 엑셀로 내보냅니다.
     */
    private void exportToExcel() {
        if (dataCollector.getResultCount() == 0) {
            JOptionPane.showMessageDialog(this, "내보낼 데이터가 없습니다.",
                    "데이터 없음", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel 파일 (*.xlsx)", "xlsx"));
        fileChooser.setSelectedFile(new File("analysis_results_" +
                System.currentTimeMillis() + ".xlsx"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            try {
                ExcelExporter exporter = new ExcelExporter();
                exporter.exportData(dataCollector.getAllResults(), file);

                JOptionPane.showMessageDialog(this,
                        "엑셀 파일이 저장되었습니다:\n" + file.getAbsolutePath(),
                        "내보내기 완료", JOptionPane.INFORMATION_MESSAGE);

                statusLabel.setText("엑셀 내보내기 완료: " + file.getName());

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "엑셀 내보내기 중 오류가 발생했습니다:\n" + e.getMessage(),
                        "내보내기 오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 프로그램 종료 시 정리 작업을 수행합니다.
     */
    public void cleanup() {
        try {
            if (yoloAnalyzer != null) {
                yoloAnalyzer.close();
            }
            if (skinAnalyzer != null) {
                skinAnalyzer.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 이미지 리스트 셀 렌더러
     */
    private static class ImageListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {

            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof ImageData) {
                ImageData imageData = (ImageData) value;
                setText(imageData.getFileName() + " (" + imageData.getFileSizeKB() + " KB)");
            }

            return this;
        }
    }
}