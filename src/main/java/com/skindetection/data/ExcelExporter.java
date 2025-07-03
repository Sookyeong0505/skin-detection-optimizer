package com.skindetection.data;

import com.skindetection.model.AnalysisResult;
import com.skindetection.model.SkinAnalysisData;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 분석 결과 데이터를 엑셀 파일로 내보내는 클래스
 * Apache POI를 사용하여 XLSX 형식의 파일을 생성합니다.
 */
public class ExcelExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 스타일 정의
    private CellStyle headerStyle;
    private CellStyle dataStyle;
    private CellStyle numberStyle;
    private CellStyle percentageStyle;
    private CellStyle dateStyle;
    private CellStyle highlightStyle;

    /**
     * 분석 결과 데이터를 엑셀 파일로 내보냅니다.
     */
    public void exportData(List<AnalysisResult> results, File outputFile) throws IOException {
        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException("내보낼 데이터가 없습니다.");
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            // 스타일 초기화
            initializeStyles(workbook);

            // 메인 데이터 시트
            createMainDataSheet(workbook, results);

            // 요약 통계 시트
            createSummarySheet(workbook, results);

            // 모델별 분석 시트
            createModelAnalysisSheet(workbook, results);

            // 임계값 분석 시트
            createThresholdAnalysisSheet(workbook, results);

            // 파일 저장
            try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
                workbook.write(fileOut);
            }

            System.out.println("엑셀 파일 생성 완료: " + outputFile.getAbsolutePath());
        }
    }

    /**
     * 셀 스타일들을 초기화합니다.
     */
    private void initializeStyles(Workbook workbook) {
        DataFormat format = workbook.createDataFormat();

        // 헤더 스타일
        headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        // 데이터 스타일
        dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);

        // 숫자 스타일
        numberStyle = workbook.createCellStyle();
        numberStyle.cloneStyleFrom(dataStyle);
        numberStyle.setDataFormat(format.getFormat("0.000"));

        // 백분율 스타일
        percentageStyle = workbook.createCellStyle();
        percentageStyle.cloneStyleFrom(dataStyle);
        percentageStyle.setDataFormat(format.getFormat("0.00%"));

        // 날짜 스타일
        dateStyle = workbook.createCellStyle();
        dateStyle.cloneStyleFrom(dataStyle);
        dateStyle.setDataFormat(format.getFormat("yyyy-mm-dd hh:mm:ss"));

        // 강조 스타일
        highlightStyle = workbook.createCellStyle();
        highlightStyle.cloneStyleFrom(dataStyle);
        highlightStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        highlightStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

    }

    /**
     * 메인 데이터 시트를 생성합니다.
     */
    private void createMainDataSheet(Workbook workbook, List<AnalysisResult> results) {
        Sheet sheet = workbook.createSheet("분석 결과 데이터");
        // 헤더 생성
        String[] headers = {"ID", "분석일시", "파일명", "파일경로", "파일크기(KB)", "모델명", "입력크기", "신뢰도임계값", "분류", "오탐특징", "탐지객체수", "탐지클래스", "최고신뢰도클래스", "최고신뢰도점수", "여성가슴신뢰도", "남성가슴신뢰도", "엉덩이신뢰도", "여성성기신뢰도", "남성성기신뢰도", "전체살색비율(%)", "Cr채널집중도(%)", "Cb채널집중도(%)", "Cr살색픽셀비율", "Cb살색픽셀비율", "25%임계값통과", "40%임계값통과", "처리시작시간(ms)", "총처리시간(ms)", "처리소요시간(초)", "종합신뢰도점수", "위험도등급", "필터권장사항"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        // 데이터 행 생성
        for (int i = 0; i < results.size(); i++) {
            AnalysisResult result = results.get(i);
            Row dataRow = sheet.createRow(i + 1);
            int colIndex = 0;
            // 기본 정보
            createCell(dataRow, colIndex++, result.getId(), dataStyle);
            createCell(dataRow, colIndex++, result.getAnalysisDate().format(DATE_FORMATTER), dataStyle);
            createCell(dataRow, colIndex++, result.getImageData() != null ? result.getImageData().getFileName() : "", dataStyle);
            createCell(dataRow, colIndex++, result.getImageData() != null ? result.getImageData().getFilePath() : "", dataStyle);
            createCell(dataRow, colIndex++, result.getImageData() != null ? result.getImageData().getFileSizeKB() : 0, numberStyle);
            // 모델 설정
            createCell(dataRow, colIndex++, result.getModelName() != null ? result.getModelName() : "", dataStyle);
            createCell(dataRow, colIndex++, result.getInputSize(), dataStyle);
            createCell(dataRow, colIndex++, result.getConfidenceThreshold(), numberStyle);
            // 분류 정보
            createCell(dataRow, colIndex++, result.getClassification() != null ? result.getClassification() : "",
                    "오탐".equals(result.getClassification()) ? highlightStyle : dataStyle);
            createCell(dataRow, colIndex++, result.getFeatures() != null ? result.getFeatures() : "", dataStyle);
            // YOLO 탐지 결과
            createCell(dataRow, colIndex++, result.getDetectedObjectsCount(), dataStyle);
            createCell(dataRow, colIndex++, result.getDetectedClasses() != null ? result.getDetectedClasses() : "", dataStyle);
            createCell(dataRow, colIndex++, result.getHighestConfidenceDetection() != null ?
                    result.getHighestConfidenceDetection().getClassName() : "", dataStyle);
            createCell(dataRow, colIndex++, result.getHighestConfidenceDetection() != null ?
                    result.getHighestConfidenceDetection().getConfidence() : 0, numberStyle);
            // 클래스별 신뢰도
            createCell(dataRow, colIndex++, result.getExposedBreastFConfidence(), numberStyle);
            createCell(dataRow, colIndex++, result.getExposedBreastMConfidence(), numberStyle);
            createCell(dataRow, colIndex++, result.getExposedButtocksConfidence(), numberStyle);
            createCell(dataRow, colIndex++, result.getExposedGenitaliaFConfidence(), numberStyle);
            createCell(dataRow, colIndex++, result.getExposedGenitaliaMConfidence(), numberStyle);
            // 살색 분석 결과
            SkinAnalysisData skinData = result.getSkinAnalysisData();
            if (skinData != null) {
                createCell(dataRow, colIndex++, skinData.getTotalSkinRatio(), numberStyle);
                createCell(dataRow, colIndex++, skinData.getCrChannelConcentration(), numberStyle);
                createCell(dataRow, colIndex++, skinData.getCbChannelConcentration(), numberStyle);
                createCell(dataRow, colIndex++, skinData.getCrSkinPixelRatio(), numberStyle);
                createCell(dataRow, colIndex++, skinData.getCbSkinPixelRatio(), numberStyle);
                createCell(dataRow, colIndex++, skinData.isThreshold25Pass() ? "통과" : "미통과", skinData.isThreshold25Pass() ? highlightStyle : dataStyle);
                createCell(dataRow, colIndex++, skinData.isThreshold40Pass() ? "통과" : "미통과", skinData.isThreshold40Pass() ? highlightStyle : dataStyle);
            } else {
                // 빈 셀들 생성
                for (int j = 0; j < 7; j++) {
                    createCell(dataRow, colIndex++, "", dataStyle);
                }
            }
            // 성능 지표
            createCell(dataRow, colIndex++, result.getProcessingStartTime(), dataStyle);
            createCell(dataRow, colIndex++, result.getTotalProcessingTime(), dataStyle);
            createCell(dataRow, colIndex++, result.getProcessingDurationSec(), numberStyle);
//             종합 평가
            createCell(dataRow, colIndex++, result.getSkinConfidenceCombined(), numberStyle);
            createCell(dataRow, colIndex++, result.getRiskLevel() != null ? result.getRiskLevel() : "",
                    getRiskLevelStyle(result.getRiskLevel()));
            createCell(dataRow, colIndex++, result.getFilterRecommendation() != null ? result.getFilterRecommendation() : "", dataStyle);
        }
        // 열 너비 자동 조정
        for (int i = 0;
             i < headers.length;
             i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 요약 통계 시트를 생성합니다.
     */
    private void createSummarySheet(Workbook workbook, List<AnalysisResult> results) {
        Sheet sheet = workbook.createSheet("요약 통계");
        int rowIndex = 0;
        // 제목
        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("분석 결과 요약 통계");
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
        rowIndex++;

        // 빈 행
        // 전체 통계
        addSummarySection(
                sheet,
                rowIndex,
                "전체 통계",
                new String[][]{
                        {"분석 건수", String.valueOf(results.size())},
                        {"정탐 건수", String.valueOf(results.stream().filter(r -> "정탐".equals(r.getClassification())).count())},
                        {"오탐 건수", String.valueOf(results.stream().filter(r -> "오탐".equals(r.getClassification())).count())},
                        {"정확도", String.format("%.2f%%", results.stream().filter(r -> "정탐".equals(r.getClassification())).count() * 100.0 / results.size())}
                });
        rowIndex += 5;
        //신뢰도 통계
        java.util.DoubleSummaryStatistics confidenceStats = results.stream().mapToDouble(AnalysisResult::getSkinConfidenceCombined).summaryStatistics();

        addSummarySection(sheet, rowIndex, "신뢰도 통계", new String[][]{
                {"평균 신뢰도", String.format("%.3f", confidenceStats.getAverage())},
                {"최고 신뢰도", String.format("%.3f", confidenceStats.getMax())},
                {"최저 신뢰도", String.format("%.3f", confidenceStats.getMin())}
        });
        rowIndex += 5;
        //살색 비율
        //통계
        java.util.DoubleSummaryStatistics skinStats = results.stream()
                .filter(r -> r.getSkinAnalysisData() != null).mapToDouble(r -> r.getSkinAnalysisData().getTotalSkinRatio()).summaryStatistics();

        addSummarySection(sheet, rowIndex, "살색 비율 통계", new String[][]{
                {"평균 살색 비율", String.format("%.2f%%", skinStats.getAverage())},
                {"최고 살색 비율", String.format("%.2f%%", skinStats.getMax())},
                {"최저 살색 비율", String.format("%.2f%%", skinStats.getMin())}
        });

        //열 너비 조정
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    /**
     * 모델별 분석 시트를 생성합니다.
     */
    private void createModelAnalysisSheet(Workbook workbook, List<AnalysisResult> results) {
        Sheet sheet = workbook.createSheet("모델별 분석");
        // 모델별 그룹화
        java.util.Map<String, List<AnalysisResult>> modelGroups = results.stream().collect(java.util.stream.Collectors.groupingBy(r -> r.getModelName() != null ? r.getModelName() : "Unknown"));
        // 헤더
        String[] headers = {"모델명", "분석 건수", "정탐 건수", "오탐 건수", "정확도", "평균 신뢰도", "평균 처리시간(초)"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0;
             i < headers.length;
             i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        int rowIndex = 1;
        for (java.util.Map.Entry<String, List<AnalysisResult>> entry : modelGroups.entrySet()) {
            String modelName = entry.getKey();
            List<AnalysisResult> modelResults = entry.getValue();
            long truePositives = modelResults.stream().mapToInt(r -> "정탐".equals(r.getClassification()) ? 1 : 0).sum();
            long falsePositives = modelResults.stream().mapToInt(r -> "오탐".equals(r.getClassification()) ? 1 : 0).sum();
            double accuracy = modelResults.size() > 0 ? (double) truePositives / modelResults.size() : 0;
            double avgConfidence = modelResults.stream().mapToDouble(AnalysisResult::getSkinConfidenceCombined).average().orElse(0);
            double avgProcessingTime = modelResults.stream().mapToDouble(AnalysisResult::getProcessingDurationSec).average().orElse(0);
            Row dataRow = sheet.createRow(rowIndex++);
            int colIndex = 0;
            createCell(dataRow, colIndex++, modelName, dataStyle);
            createCell(dataRow, colIndex++, modelResults.size(), dataStyle);
            createCell(dataRow, colIndex++, (int) truePositives, dataStyle);
            createCell(dataRow, colIndex++, (int) falsePositives, dataStyle);
            createCell(dataRow, colIndex++, accuracy, percentageStyle);
            createCell(dataRow, colIndex++, avgConfidence, numberStyle);
            createCell(dataRow, colIndex++, avgProcessingTime, numberStyle);
        }
        // 열 너비 자동 조정
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 임계값 분석 시트를 생성합니다.
     */
    private void createThresholdAnalysisSheet(Workbook workbook, List<AnalysisResult> results) {
        Sheet sheet = workbook.createSheet("임계값 분석");
        // 다양한 임계값에 대한 분석
        double[] thresholds = {0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.55, 0.6, 0.65, 0.7};
        // 헤더
         String[] headers = {"임계값", "통과 건수", "정탐 중 통과", "오탐 중 통과", "정탐률", "오탐률", "정확도"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        for (int i = 0; i < thresholds.length; i++) {
            double threshold = thresholds[i];
            List<AnalysisResult> passedResults = results.stream()
                    .filter(r -> r.getSkinAnalysisData() != null &&
                            r.getSkinAnalysisData().getTotalSkinRatio() / 100.0 >= threshold)
                    .collect(java.util.stream.Collectors.toList());
            long truePositivesPassed = passedResults.stream().mapToInt(r -> "정탐".equals(r.getClassification()) ? 1 : 0).sum();
            long falsePositivesPassed = passedResults.stream().mapToInt(r -> "오탐".equals(r.getClassification()) ? 1 : 0).sum();
            long totalTruePositives = results.stream().mapToInt(r -> "정탐".equals(r.getClassification()) ? 1 : 0).sum();
            long totalFalsePositives = results.stream().mapToInt(r -> "오탐".equals(r.getClassification()) ? 1 : 0).sum();
            double truePositiveRate = totalTruePositives > 0 ? (double) truePositivesPassed / totalTruePositives : 0;
            double falsePositiveRate = totalFalsePositives > 0 ? (double) falsePositivesPassed / totalFalsePositives : 0;
            double accuracy = results.size() > 0 ? (double) (totalTruePositives - falsePositivesPassed + totalFalsePositives - truePositivesPassed) / results.size() : 0;
            Row dataRow = sheet.createRow(i + 1);
            int colIndex = 0;
            createCell(dataRow, colIndex++, threshold, numberStyle);
            createCell(dataRow, colIndex++, passedResults.size(), dataStyle);
            createCell(dataRow, colIndex++, (int) truePositivesPassed, dataStyle);
            createCell(dataRow, colIndex++, (int) falsePositivesPassed, dataStyle);
            createCell(dataRow, colIndex++, truePositiveRate, percentageStyle);
            createCell(dataRow, colIndex++, falsePositiveRate, percentageStyle);
            createCell(dataRow, colIndex++, accuracy, percentageStyle);
        }

        // 열 너비 자동 조정
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 요약 섹션을 추가합니다.
     */
    private void addSummarySection(Sheet sheet, int startRow, String title, String[][] data) {
        // 섹션 제목
        Row titleRow = sheet.createRow(startRow);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(headerStyle);
        // 데이터 행들
        for (int i = 0; i < data.length; i++) {
            Row dataRow = sheet.createRow(startRow + i + 1);
            createCell(dataRow, 0, data[i][0], dataStyle);
            createCell(dataRow, 1, data[i][1], dataStyle);
        }
    }

    /**
     * 셀을 생성하고 값을 설정합니다.
     */
    private void createCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else {
            cell.setCellValue(value != null ? value.toString() : "");
        }
        cell.setCellStyle(style);
    }

    /**
     * 위험도에 따른 스타일을 반환합니다.
     */
    private CellStyle getRiskLevelStyle(String riskLevel) {
        if ("HIGH".equals(riskLevel)) {
            return highlightStyle;
            // 빨간색 강조
        }
        return dataStyle;
    }
}
