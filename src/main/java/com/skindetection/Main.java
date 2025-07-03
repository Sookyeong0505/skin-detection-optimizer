package com.skindetection;

import com.skindetection.gui.MainFrame;
import com.skindetection.utils.ImageUtils;
import org.opencv.core.Core;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * 선정성 탐지 최적화 프로그램 메인 클래스
 * YOLO 모델과 살색 필터링을 통한 오탐률 감소 시스템
 */
public class Main {

    static {
        // OpenCV 네이티브 라이브러리 로드
        try {
            nu.pattern.OpenCV.loadLocally();
        } catch (Exception e) {
            System.err.println("OpenCV 로드 실패: " + e.getMessage());
            // 시스템에 설치된 OpenCV 사용 시도
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        }
    }

    public static void main(String[] args) {
        // Look and Feel 설정
        try {
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Look and Feel 설정 실패: " + e.getMessage());
        }

        // WebP 이미지 지원 초기화
        ImageUtils.initializeWebPSupport();

        // 필요한 디렉토리 생성
        createRequiredDirectories();

        // 메인 프레임 실행
        SwingUtilities.invokeLater(() -> {
            try {
                MainFrame mainFrame = new MainFrame();

                // 화면 중앙에 위치
                mainFrame.setLocationRelativeTo(null);

                // 프로그램 종료 시 정리 작업
                mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println("프로그램을 종료합니다...");
                    mainFrame.cleanup();
                }));

                mainFrame.setVisible(true);

                System.out.println("선정성 탐지 최적화 프로그램이 시작되었습니다.");

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        "프로그램 시작 중 오류가 발생했습니다:\n" + e.getMessage(),
                        "시작 오류",
                        JOptionPane.ERROR_MESSAGE
                );
                System.exit(1);
            }
        });
    }

    /**
     * 프로그램 실행에 필요한 디렉토리들을 생성합니다.
     */
    private static void createRequiredDirectories() {
        String[] directories = {
                "data/input",
                "data/output",
                "data/results",
                "models",
                "temp"
        };

        for (String dir : directories) {
            File directory = new File(dir);
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                if (created) {
                    System.out.println("디렉토리 생성: " + dir);
                } else {
                    System.err.println("디렉토리 생성 실패: " + dir);
                }
            }
        }
    }

    /**
     * 프로그램 정보를 출력합니다.
     */
    public static void printProgramInfo() {
        System.out.println("==========================================");
        System.out.println("  선정성 탐지 최적화 프로그램 v1.0");
        System.out.println("  YOLO + 살색 필터링 임계값 최적화");
        System.out.println("==========================================");
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("Available Processors: " + Runtime.getRuntime().availableProcessors());
        System.out.println("Max Memory: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " MB");
        System.out.println("==========================================");
    }
}