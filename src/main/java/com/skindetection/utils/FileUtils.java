package com.skindetection.utils;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 파일 처리를 위한 유틸리티 클래스
 * 파일 검색, 복사, 삭제, 압축 등의 기능을 제공합니다.
 */
public class FileUtils {

    // 지원되는 이미지 확장자
    private static final Set<String> SUPPORTED_IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "bmp", "gif", "tiff", "tif"
    );

    // 지원되는 모델 확장자
    private static final Set<String> SUPPORTED_MODEL_EXTENSIONS = Set.of(
            "onnx", "pt", "pth", "pb", "tflite"
    );

    // 최대 파일 크기 (100MB)
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024;

    /**
     * 디렉토리가 존재하지 않으면 생성합니다.
     */
    public static boolean createDirectoryIfNotExists(String dirPath) {
        return createDirectoryIfNotExists(new File(dirPath));
    }

    /**
     * 디렉토리가 존재하지 않으면 생성합니다.
     */
    public static boolean createDirectoryIfNotExists(File directory) {
        if (directory == null) {
            return false;
        }

        if (!directory.exists()) {
            return directory.mkdirs();
        }

        return directory.isDirectory();
    }

    /**
     * 지정된 디렉토리에서 이미지 파일들을 찾습니다.
     */
    public static List<File> findImageFiles(File directory) {
        return findImageFiles(directory, false);
    }

    /**
     * 지정된 디렉토리에서 이미지 파일들을 찾습니다.
     */
    public static List<File> findImageFiles(File directory, boolean recursive) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return new ArrayList<>();
        }

        List<File> imageFiles = new ArrayList<>();

        try {
            Files.walkFileTree(directory.toPath(),
                    recursive ? EnumSet.of(FileVisitOption.FOLLOW_LINKS) : EnumSet.noneOf(FileVisitOption.class),
                    recursive ? Integer.MAX_VALUE : 1,
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (isImageFile(file.toFile())) {
                                imageFiles.add(file.toFile());
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            System.err.println("이미지 파일 검색 중 오류: " + e.getMessage());
        }

        // 파일명으로 정렬
        imageFiles.sort(Comparator.comparing(File::getName));

        return imageFiles;
    }

    /**
     * 지정된 디렉토리에서 모델 파일들을 찾습니다.
     */
    public static List<File> findModelFiles(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return new ArrayList<>();
        }

        List<File> modelFiles = new ArrayList<>();

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && isModelFile(file)) {
                    modelFiles.add(file);
                }
            }
        }

        modelFiles.sort(Comparator.comparing(File::getName));

        return modelFiles;
    }

    /**
     * 파일이 이미지 파일인지 확인합니다.
     */
    public static boolean isImageFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }

        String extension = getFileExtension(file).toLowerCase();
        return SUPPORTED_IMAGE_EXTENSIONS.contains(extension);
    }

    /**
     * 파일이 모델 파일인지 확인합니다.
     */
    public static boolean isModelFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }

        String extension = getFileExtension(file).toLowerCase();
        return SUPPORTED_MODEL_EXTENSIONS.contains(extension);
    }

    /**
     * 파일 확장자를 반환합니다.
     */
    public static String getFileExtension(File file) {
        if (file == null) {
            return "";
        }

        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot + 1) : "";
    }

    /**
     * 파일명에서 확장자를 제거합니다.
     */
    public static String getFileNameWithoutExtension(File file) {
        if (file == null) {
            return "";
        }

        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(0, lastDot) : name;
    }

    /**
     * 파일 크기를 사람이 읽기 쉬운 형태로 변환합니다.
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 파일이 처리 가능한 크기인지 확인합니다.
     */
    public static boolean isFileSizeValid(File file) {
        return file != null && file.exists() && file.length() <= MAX_FILE_SIZE;
    }

    /**
     * 파일을 다른 위치로 복사합니다.
     */
    public static boolean copyFile(File source, File destination) {
        if (source == null || !source.exists() || destination == null) {
            return false;
        }

        try {
            // 대상 디렉토리 생성
            File parentDir = destination.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;

        } catch (IOException e) {
            System.err.println("파일 복사 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 파일을 이동합니다.
     */
    public static boolean moveFile(File source, File destination) {
        if (copyFile(source, destination)) {
            return source.delete();
        }
        return false;
    }

    /**
     * 파일을 안전하게 삭제합니다.
     */
    public static boolean deleteFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }

        try {
            Files.delete(file.toPath());
            return true;
        } catch (IOException e) {
            System.err.println("파일 삭제 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 디렉토리를 재귀적으로 삭제합니다.
     */
    public static boolean deleteDirectory(File directory) {
        if (directory == null || !directory.exists()) {
            return false;
        }

        try {
            Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            return true;
        } catch (IOException e) {
            System.err.println("디렉토리 삭제 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 파일의 MD5 해시를 계산합니다.
     */
    public static String calculateMD5(File file) {
        if (file == null || !file.exists()) {
            return null;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }

            byte[] hash = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("MD5 계산 실패: " + e.getMessage());
            return null;
        }
    }

    /**
     * 파일들을 ZIP으로 압축합니다.
     */
    public static boolean createZipArchive(List<File> files, File zipFile) {
        if (files == null || files.isEmpty() || zipFile == null) {
            return false;
        }

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {

            for (File file : files) {
                if (file.exists() && file.isFile()) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        ZipEntry entry = new ZipEntry(file.getName());
                        zos.putNextEntry(entry);

                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            zos.write(buffer, 0, bytesRead);
                        }

                        zos.closeEntry();
                    }
                }
            }

            return true;

        } catch (IOException e) {
            System.err.println("ZIP 생성 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 임시 파일을 생성합니다.
     */
    public static File createTempFile(String prefix, String suffix) {
        try {
            File tempFile = File.createTempFile(prefix, suffix);
            tempFile.deleteOnExit(); // JVM 종료 시 자동 삭제
            return tempFile;
        } catch (IOException e) {
            System.err.println("임시 파일 생성 실패: " + e.getMessage());
            return null;
        }
    }

    /**
     * 고유한 파일명을 생성합니다.
     */
    public static String generateUniqueFileName(String baseName, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String randomSuffix = String.valueOf((int) (Math.random() * 1000));

        return String.format("%s_%s_%s.%s", baseName, timestamp, randomSuffix, extension);
    }

    /**
     * 파일명이 안전한지 확인합니다 (경로 탐색 공격 방지).
     */
    public static boolean isSafeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }

        // 위험한 문자들 확인
        String[] dangerousChars = {"..", "/", "\\", ":", "*", "?", "\"", "<", ">", "|"};
        for (String dangerous : dangerousChars) {
            if (fileName.contains(dangerous)) {
                return false;
            }
        }

        // 예약된 이름들 확인 (Windows)
        String[] reservedNames = {"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3",
                "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
                "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6",
                "LPT7", "LPT8", "LPT9"};

        String nameWithoutExt = getFileNameWithoutExtension(new File(fileName)).toUpperCase();
        for (String reserved : reservedNames) {
            if (nameWithoutExt.equals(reserved)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 파일 정보를 담는 클래스
     */
    public static class FileInfo {
        private final File file;
        private final long size;
        private final LocalDateTime lastModified;
        private final String md5Hash;

        public FileInfo(File file) {
            this.file = file;
            this.size = file.exists() ? file.length() : 0;
            this.lastModified = file.exists() ?
                    LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(file.lastModified()),
                            java.time.ZoneId.systemDefault()) : null;
            this.md5Hash = calculateMD5(file);
        }

        public File getFile() { return file; }
        public long getSize() { return size; }
        public LocalDateTime getLastModified() { return lastModified; }
        public String getMd5Hash() { return md5Hash; }
        public String getFormattedSize() { return formatFileSize(size); }

        @Override
        public String toString() {
            return String.format("FileInfo{name='%s', size=%s, modified=%s}",
                    file.getName(), getFormattedSize(), lastModified);
        }
    }

    /**
     * 디렉토리 통계 정보를 담는 클래스
     */
    public static class DirectoryStats {
        private final int totalFiles;
        private final int imageFiles;
        private final int modelFiles;
        private final long totalSize;

        public DirectoryStats(File directory) {
            if (directory == null || !directory.exists() || !directory.isDirectory()) {
                this.totalFiles = 0;
                this.imageFiles = 0;
                this.modelFiles = 0;
                this.totalSize = 0;
                return;
            }

            File[] files = directory.listFiles();
            if (files == null) {
                this.totalFiles = 0;
                this.imageFiles = 0;
                this.modelFiles = 0;
                this.totalSize = 0;
                return;
            }

            int totalCount = 0;
            int imageCount = 0;
            int modelCount = 0;
            long sizeSum = 0;

            for (File file : files) {
                if (file.isFile()) {
                    totalCount++;
                    sizeSum += file.length();

                    if (isImageFile(file)) {
                        imageCount++;
                    } else if (isModelFile(file)) {
                        modelCount++;
                    }
                }
            }

            this.totalFiles = totalCount;
            this.imageFiles = imageCount;
            this.modelFiles = modelCount;
            this.totalSize = sizeSum;
        }

        public int getTotalFiles() { return totalFiles; }
        public int getImageFiles() { return imageFiles; }
        public int getModelFiles() { return modelFiles; }
        public long getTotalSize() { return totalSize; }
        public String getFormattedTotalSize() { return formatFileSize(totalSize); }

        @Override
        public String toString() {
            return String.format("DirectoryStats{total=%d, images=%d, models=%d, size=%s}",
                    totalFiles, imageFiles, modelFiles, getFormattedTotalSize());
        }
    }

    /**
     * 파일 필터 유틸리티
     */
    public static class FileFilters {

        public static FileFilter imageFilter() {
            return file -> file.isFile() && isImageFile(file);
        }

        public static FileFilter modelFilter() {
            return file -> file.isFile() && isModelFile(file);
        }

        public static FileFilter sizeFilter(long maxSize) {
            return file -> file.isFile() && file.length() <= maxSize;
        }

        public static FileFilter extensionFilter(String... extensions) {
            Set<String> extSet = Arrays.stream(extensions)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            return file -> file.isFile() && extSet.contains(getFileExtension(file).toLowerCase());
        }
    }
}