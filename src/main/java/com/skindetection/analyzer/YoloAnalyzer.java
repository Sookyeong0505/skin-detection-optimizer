package com.skindetection.analyzer;

import ai.onnxruntime.*;
import com.skindetection.model.DetectionObject;
import com.skindetection.utils.ImageUtils;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.nio.FloatBuffer;
import java.util.*;

/**
 * YOLO 모델을 사용한 객체 탐지 분석기
 * ONNX Runtime을 통해 선정적 객체를 탐지합니다.
 */
public class YoloAnalyzer implements AutoCloseable {

    // YOLO 모델 설정
    private static final int INPUT_SIZE = 640;
    private static final float CONFIDENCE_THRESHOLD = 0.5f;
    private static final float NMS_THRESHOLD = 0.4f;

    // 탐지 클래스 정의
    private static final Map<Integer, String> CLASS_NAMES = Map.of(
            0, "EXPOSED_BREAST_F",
            1, "EXPOSED_BREAST_M",
            2, "EXPOSED_BUTTOCKS",
            3, "EXPOSED_GENITALIA_F",
            4, "EXPOSED_GENITALIA_M"
    );

    private OrtEnvironment environment;
    private OrtSession session;
    private String currentModelPath;

    public YoloAnalyzer() throws OrtException {
        this.environment = OrtEnvironment.getEnvironment();
        loadDefaultModel();
    }

    /**
     * 기본 모델을 로드합니다.
     */
    private void loadDefaultModel() throws OrtException {
        loadModel("models/best_640n_0522.onnx");
    }

    /**
     * 지정된 모델을 로드합니다.
     */
    public void loadModel(String modelPath) throws OrtException {
        if (session != null) {
            session.close();
        }

        File modelFile = new File(modelPath);
        if (!modelFile.exists()) {
            throw new RuntimeException("모델 파일을 찾을 수 없습니다: " + modelPath);
        }

        OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
        sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);

        this.session = environment.createSession(modelPath, sessionOptions);
        this.currentModelPath = modelPath;

        System.out.println("YOLO 모델 로드 완료: " + modelPath);
    }

    /**
     * 이미지 파일을 분석하여 객체 탐지를 수행합니다.
     */
    public List<DetectionObject> analyze(File imageFile, String modelName) throws Exception {
        // 필요시 모델 변경
        String modelPath = "models/" + modelName;
        if (!modelPath.equals(currentModelPath)) {
            loadModel(modelPath);
        }

        // 이미지 로드 및 전처리
        Mat image = loadAndPreprocessImage(imageFile);
        if (image.empty()) {
            throw new RuntimeException("이미지를 로드할 수 없습니다: " + imageFile.getPath());
        }

        // 원본 이미지 크기 저장
        Size originalSize = image.size();

        // YOLO 입력 형식으로 변환
        Mat inputBlob = prepareInputBlob(image);

        // 추론 실행
        List<DetectionObject> detections = runInference(inputBlob, originalSize);

        // NMS 적용
        List<DetectionObject> filteredDetections = applyNMS(detections);

        // 메모리 정리
        image.release();
        inputBlob.release();

        return filteredDetections;
    }

    /**
     * 이미지를 로드하고 기본 전처리를 수행합니다.
     */
    private Mat loadAndPreprocessImage(File imageFile) {
        Mat image = Imgcodecs.imread(imageFile.getAbsolutePath());

        if (image.empty()) {
            // WebP 등 특수 포맷 처리
            try {
                image = ImageUtils.loadImageAsMat(imageFile);
            } catch (Exception e) {
                System.err.println("이미지 로드 실패: " + e.getMessage());
                return new Mat();
            }
        }

        // RGB로 변환 (OpenCV는 BGR을 사용)
        Mat rgbImage = new Mat();
        Imgproc.cvtColor(image, rgbImage, Imgproc.COLOR_BGR2RGB);
        image.release();

        return rgbImage;
    }

    /**
     * YOLO 입력을 위한 블롭을 준비합니다.
     */
    private Mat prepareInputBlob(Mat image) {
        Mat resizedImage = new Mat();

        // 정사각형으로 리사이즈 (패딩 추가)
        Mat squareImage = makeSquare(image);
        Imgproc.resize(squareImage, resizedImage, new Size(INPUT_SIZE, INPUT_SIZE));
        squareImage.release();

        // 정규화 (0-255 -> 0-1)
        resizedImage.convertTo(resizedImage, CvType.CV_32F, 1.0 / 255.0);

        // HWC -> CHW 변환 및 배치 차원 추가
        Mat blob = Dnn.blobFromImage(resizedImage, 1.0,
                new Size(INPUT_SIZE, INPUT_SIZE), new Scalar(0), true, false, CvType.CV_32F);

        resizedImage.release();
        return blob;
    }

    /**
     * 이미지를 정사각형으로 만듭니다 (패딩 추가).
     */
    private Mat makeSquare(Mat image) {
        int height = (int) image.size().height;
        int width = (int) image.size().width;
        int maxSize = Math.max(height, width);

        Mat squareImage = Mat.zeros(maxSize, maxSize, image.type());

        int offsetX = (maxSize - width) / 2;
        int offsetY = (maxSize - height) / 2;

        Rect roi = new Rect(offsetX, offsetY, width, height);
        image.copyTo(squareImage.submat(roi));

        return squareImage;
    }

    /**
     * ONNX 모델 추론을 실행합니다.
     */
    private List<DetectionObject> runInference(Mat inputBlob, Size originalSize) throws OrtException {
        // 입력 텐서 생성
        long[] shape = {1, 3, INPUT_SIZE, INPUT_SIZE};
        float[] inputData = matToFloatArray(inputBlob);

        OnnxTensor inputTensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(inputData), shape);

        // 추론 실행
        Map<String, OnnxTensor> inputs = Map.of("images", inputTensor);
        OrtSession.Result result = session.run(inputs);

        // 출력 처리
        List<DetectionObject> detections = parseModelOutput(result, originalSize);

        // 리소스 정리
        inputTensor.close();
        result.close();

        return detections;
    }

    /**
     * Mat 객체를 float 배열로 변환합니다.
     */
    private float[] matToFloatArray(Mat mat) {
        int channels = mat.channels();
        int rows = mat.rows();
        int cols = mat.cols();

        float[] result = new float[channels * rows * cols];
        mat.get(0, 0, result);

        return result;
    }

    /**
     * 모델 출력을 파싱하여 탐지 객체 목록을 생성합니다.
     */
    private List<DetectionObject> parseModelOutput(OrtSession.Result result, Size originalSize) throws OrtException {
        List<DetectionObject> detections = new ArrayList<>();

        // YOLOv8 출력 형식: [1, 84, 8400] 또는 [1, num_classes + 4, num_predictions]
        OnnxTensor outputTensor = (OnnxTensor) result.get(0);
        float[][][] output = (float[][][]) outputTensor.getValue();

        int numPredictions = output[0][0].length;
        int numClasses = CLASS_NAMES.size();

        for (int i = 0; i < numPredictions; i++) {
            // 바운딩 박스 좌표 (center_x, center_y, width, height)
            float centerX = output[0][0][i];
            float centerY = output[0][1][i];
            float width = output[0][2][i];
            float height = output[0][3][i];

            // 클래스별 신뢰도 확인
            for (int classId = 0; classId < numClasses; classId++) {
                float confidence = output[0][4 + classId][i];

                if (confidence >= CONFIDENCE_THRESHOLD) {
                    // 좌표를 원본 이미지 크기로 변환
                    float x1 = (centerX - width / 2) * (float) originalSize.width / INPUT_SIZE;
                    float y1 = (centerY - height / 2) * (float) originalSize.height / INPUT_SIZE;
                    float x2 = (centerX + width / 2) * (float) originalSize.width / INPUT_SIZE;
                    float y2 = (centerY + height / 2) * (float) originalSize.height / INPUT_SIZE;

                    DetectionObject detection = new DetectionObject(
                            classId,
                            CLASS_NAMES.get(classId),
                            confidence,
                            new float[]{x1, y1, x2, y2}
                    );

                    detections.add(detection);
                }
            }
        }

        return detections;
    }

    /**
     * Non-Maximum Suppression을 적용하여 중복 탐지를 제거합니다.
     */
    private List<DetectionObject> applyNMS(List<DetectionObject> detections) {
        if (detections.isEmpty()) {
            return detections;
        }

        // 신뢰도 기준으로 정렬
        detections.sort((a, b) -> Float.compare(b.getConfidence(), a.getConfidence()));

        List<DetectionObject> filteredDetections = new ArrayList<>();
        boolean[] suppressed = new boolean[detections.size()];

        for (int i = 0; i < detections.size(); i++) {
            if (suppressed[i]) continue;

            DetectionObject current = detections.get(i);
            filteredDetections.add(current);

            for (int j = i + 1; j < detections.size(); j++) {
                if (suppressed[j]) continue;

                DetectionObject other = detections.get(j);

                // 같은 클래스이고 IoU가 임계값 이상이면 억제
                if (current.getClassId() == other.getClassId()) {
                    float iou = calculateIoU(current.getBoundingBox(), other.getBoundingBox());
                    if (iou >= NMS_THRESHOLD) {
                        suppressed[j] = true;
                    }
                }
            }
        }

        return filteredDetections;
    }

    /**
     * Intersection over Union (IoU)을 계산합니다.
     */
    private float calculateIoU(float[] box1, float[] box2) {
        float x1 = Math.max(box1[0], box2[0]);
        float y1 = Math.max(box1[1], box2[1]);
        float x2 = Math.min(box1[2], box2[2]);
        float y2 = Math.min(box1[3], box2[3]);

        if (x2 <= x1 || y2 <= y1) {
            return 0.0f;
        }

        float intersection = (x2 - x1) * (y2 - y1);
        float area1 = (box1[2] - box1[0]) * (box1[3] - box1[1]);
        float area2 = (box2[2] - box2[0]) * (box2[3] - box2[1]);
        float union = area1 + area2 - intersection;

        return intersection / union;
    }

    /**
     * 현재 로드된 모델 정보를 반환합니다.
     */
    public String getCurrentModelInfo() {
        if (session == null) {
            return "모델이 로드되지 않음";
        }

        try {
            Map<String, NodeInfo> inputInfo = session.getInputInfo();
            Map<String, NodeInfo> outputInfo = session.getOutputInfo();

            StringBuilder info = new StringBuilder();
            info.append("모델: ").append(currentModelPath).append("\n");
            info.append("입력: ").append(inputInfo.keySet()).append("\n");
            info.append("출력: ").append(outputInfo.keySet()).append("\n");
            info.append("클래스 수: ").append(CLASS_NAMES.size()).append("\n");
            info.append("신뢰도 임계값: ").append(CONFIDENCE_THRESHOLD).append("\n");
            info.append("NMS 임계값: ").append(NMS_THRESHOLD);

            return info.toString();

        } catch (OrtException e) {
            return "모델 정보 조회 실패: " + e.getMessage();
        }
    }

    /**
     * 탐지 가능한 클래스 목록을 반환합니다.
     */
    public Map<Integer, String> getClassNames() {
        return new HashMap<>(CLASS_NAMES);
    }

    /**
     * 신뢰도 임계값을 설정합니다.
     */
    public void setConfidenceThreshold(float threshold) {
        // 동적 임계값 변경이 필요한 경우 구현
        System.out.println("신뢰도 임계값 변경: " + threshold);
    }

    @Override
    public void close() throws Exception {
        if (session != null) {
            session.close();
        }
        if (environment != null) {
            // OrtEnvironment는 전역적으로 관리되므로 일반적으로 close하지 않음
        }
        System.out.println("YOLO 분석기가 종료되었습니다.");
    }
}