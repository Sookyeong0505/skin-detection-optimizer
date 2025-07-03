# 선정성 탐지 최적화 프로그램

[![Java](https://img.shields.io/badge/Java-11+-orange.svg)](https://openjdk.java.net/)
[![OpenCV](https://img.shields.io/badge/OpenCV-4.8.0-blue.svg)](https://opencv.org/)
[![ONNX Runtime](https://img.shields.io/badge/ONNX%20Runtime-1.16.3-green.svg)](https://onnxruntime.ai/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> YOLO 기반 객체 탐지와 YCrCb 색공간 살색 분석을 통한 선정성 탐지 오탐률 감소 시스템

## 📋 목차

- [프로젝트 개요](#-프로젝트-개요)
- [주요 기능](#-주요-기능)
- [시스템 요구사항](#-시스템-요구사항)
- [설치 방법](#-설치-방법)
- [사용 방법](#-사용-방법)
- [프로젝트 구조](#-프로젝트-구조)
- [설정 옵션](#-설정-옵션)
- [데이터 분석](#-데이터-분석)
- [문제 해결](#-문제-해결)
- [기여 방법](#-기여-방법)
- [라이선스](#-라이선스)

## 🎯 프로젝트 개요

**선정성 탐지 최적화 프로그램**은 AI 기반 선정성 탐지 시스템의 오탐률을 줄이기 위해 개발된 데이터 분석 도구입니다. 

### 배경

기존 AI 모델이 노출하지 않은 신체 부위를 탐지하거나 선정적이지 않은 이미지를 오탐하는 문제를 해결하기 위해, YOLO 객체 탐지와 YCrCb 색공간 살색 분석을 결합한 필터링 시스템을 구축하여 최적의 임계값을 찾는 것이 목적입니다.

### 핵심 아이디어

- **YOLO 모델**: `best_640n_0522.onnx`를 사용하여 노출된 신체 부위 탐지
- **살색 분석**: YCrCb 색공간과 히스토그램 분석을 통한 살색 비율 계산
- **임계값 최적화**: 정탐 데이터와 오탐 데이터를 구분하는 최적 임계값 도출

## ✨ 주요 기능

### 🔍 분석 기능
- **YOLO 객체 탐지**: EXPOSED_BREAST, EXPOSED_BUTTOCKS, EXPOSED_GENITALIA 등 탐지
- **살색 분석**: YCrCb 색공간에서 살색 비율 및 집중도 계산
- **통합 분석**: 객체 탐지와 살색 분석 결과를 종합한 신뢰도 점수 산출

### 📊 데이터 수집
- **배치 처리**: 여러 이미지 파일 동시 분석
- **분류 기능**: 정탐/오탐 데이터 분류 및 특징 기록
- **실시간 통계**: 분석 진행 상황 및 성능 지표 모니터링

### 📈 결과 분석
- **시각화**: 탐지 결과 오버레이, 살색 분포 차트
- **엑셀 내보내기**: 상세 분석 결과를 Excel 파일로 저장
- **임계값 분석**: 다양한 임계값에 대한 성능 평가

### 🎨 사용자 인터페이스
- **직관적 GUI**: Swing 기반의 사용자 친화적 인터페이스
- **실시간 미리보기**: 이미지 및 분석 결과 실시간 표시
- **진행 상황 표시**: 프로그레스 바를 통한 작업 진행률 확인

## 💻 시스템 요구사항

### 최소 요구사항
- **운영체제**: Windows 10, macOS 10.14, Ubuntu 18.04 이상
- **Java**: OpenJDK 11 이상
- **메모리**: 4GB RAM
- **저장공간**: 2GB 이상
- **해상도**: 1024x768 이상

### 권장 요구사항
- **운영체제**: Windows 11, macOS 12, Ubuntu 20.04 이상
- **Java**: OpenJDK 17 이상
- **메모리**: 8GB RAM 이상
- **저장공간**: 10GB 이상
- **해상도**: 1920x1080 이상
- **GPU**: CUDA 지원 GPU (향후 가속화 지원 예정)

## 🚀 설치 방법

### 1. 사전 준비

```bash
# Java 11 이상 설치 확인
java -version

# Git 클론
git clone https://github.com/your-username/skin-detection-optimizer.git
cd skin-detection-optimizer
```

### 2. 빌드 방법

#### Maven 사용
```bash
# 의존성 설치 및 컴파일
mvn clean compile

# 전체 빌드 (테스트 포함)
mvn clean package

# 테스트 제외하고 빌드
mvn clean package -DskipTests

# 실행 가능한 JAR 파일 생성
mvn clean package -Pprod
```

#### IDE 사용 (IntelliJ IDEA)
1. IntelliJ IDEA에서 `File > Open` 선택
2. 프로젝트 루트 디렉토리 선택
3. Maven 프로젝트로 자동 인식됨
4. `Run > Run 'Main'`으로 실행

### 3. 모델 파일 준비

```bash
# models 디렉토리 생성
mkdir -p models

# YOLO 모델 파일 복사
cp /path/to/your/best_640n_0522.onnx models/
```

## 📘 사용 방법

### 1. 프로그램 실행

```bash
# Maven을 통한 실행
mvn exec:java -Dexec.mainClass="com.skindetection.Main"

# JAR 파일 실행
java -jar target/skin-detection-optimizer-1.0.0.jar

# 메모리 옵션과 함께 실행
java -Xmx4g -jar target/skin-detection-optimizer-1.0.0.jar
```

### 2. 기본 워크플로우

#### Step 1: 모델 선택
- 상단 콤보박스에서 사용할 YOLO 모델 선택
- 기본값: `best_640n_0522.onnx`

#### Step 2: 이미지 선택
- "이미지 선택" 버튼 클릭
- 지원 형식: WebP, JPG, PNG, BMP, GIF, TIFF
- 여러 파일 동시 선택 가능

#### Step 3: 분류 설정
- **정탐 (True Positive)**: 올바르게 선정적 콘텐츠로 탐지된 경우
- **오탐 (False Positive)**: 잘못 탐지된 경우
  - 오탐 선택 시 특징 설명 입력 필수

#### Step 4: 분석 실행
- "분석 시작" 버튼 클릭
- 진행 상황은 프로그레스 바로 확인

#### Step 5: 결과 확인
- **이미지 뷰어**: 탐지된 객체가 표시된 이미지
- **YOLO 결과**: 탐지된 객체 목록 및 신뢰도
- **살색 분석**: 살색 비율, Cr/Cb 채널 집중도
- **종합 평가**: 신뢰도 점수, 위험도, 권장사항

#### Step 6: 데이터 저장
- "결과 저장" 버튼으로 분석 데이터 수집
- 여러 이미지 분석 후 "엑셀로 다운로드"

### 3. 고급 기능

#### 배치 처리
```java
// 여러 이미지를 한 번에 처리
File[] imageFiles = {image1, image2, image3};
for (File imageFile : imageFiles) {
    AnalysisResult result = analyzer.analyze(imageFile);
    dataCollector.addResult(result);
}
```

#### 사용자 정의 임계값
```properties
# application.properties에서 임계값 조정
skin.threshold.custom=0.35
yolo.confidence.threshold=0.6
```

## 🏗️ 프로젝트 구조

```
skin-detection-optimizer/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/skindetection/
│   │   │       ├── Main.java                 # 애플리케이션 진입점
│   │   │       ├── gui/                      # GUI 관련 클래스
│   │   │       │   ├── MainFrame.java        # 메인 윈도우
│   │   │       │   ├── ImagePanel.java       # 이미지 표시 패널
│   │   │       │   └── ResultPanel.java      # 결과 표시 패널
│   │   │       ├── analyzer/                 # 분석기 클래스
│   │   │       │   ├── YoloAnalyzer.java     # YOLO 모델 분석기
│   │   │       │   ├── SkinColorAnalyzer.java # 살색 분석기
│   │   │       │   └── ImageProcessor.java   # 이미지 전처리기
│   │   │       ├── model/                    # 데이터 모델
│   │   │       │   ├── AnalysisResult.java   # 분석 결과
│   │   │       │   ├── DetectionObject.java  # 탐지 객체
│   │   │       │   ├── SkinAnalysisData.java # 살색 분석 데이터
│   │   │       │   └── ImageData.java        # 이미지 메타데이터
│   │   │       ├── data/                     # 데이터 관리
│   │   │       │   ├── DataCollector.java    # 데이터 수집기
│   │   │       │   └── ExcelExporter.java    # 엑셀 내보내기
│   │   │       └── utils/                    # 유틸리티
│   │   │           ├── ImageUtils.java       # 이미지 처리
│   │   │           ├── ColorSpaceUtils.java  # 색공간 변환
│   │   │           └── FileUtils.java        # 파일 처리
│   │   └── resources/
│   │       ├── application.properties        # 설정 파일
│   │       ├── logback.xml                  # 로깅 설정
│   │       └── models/                      # 모델 파일 (gitignore)
│   └── test/                                # 테스트 코드
├── data/                                    # 데이터 디렉토리
│   ├── input/                              # 입력 이미지
│   ├── output/                             # 출력 결과
│   ├── results/                            # 분석 결과
│   └── backup/                             # 백업 파일
├── logs/                                   # 로그 파일
├── models/                                 # YOLO 모델 파일
├── pom.xml                                 # Maven 설정
└── README.md                               # 프로젝트 문서
```

### 주요 패키지 설명

#### `com.skindetection.gui`
- 사용자 인터페이스 관련 클래스
- Swing 기반의 GUI 컴포넌트들

#### `com.skindetection.analyzer`
- 핵심 분석 로직
- YOLO 모델 추론, 살색 분석, 이미지 전처리

#### `com.skindetection.model`
- 데이터 모델 및 구조체
- 분석 결과, 이미지 정보 등의 데이터 클래스

#### `com.skindetection.data`
- 데이터 수집 및 내보내기
- Excel 파일 생성, 통계 계산

#### `com.skindetection.utils`
- 범용 유틸리티 함수
- 이미지 처리, 파일 조작, 색공간 변환

## ⚙️ 설정 옵션

### application.properties 주요 설정

```properties
# YOLO 모델 설정
yolo.default.model=models/best_640n_0522.onnx
yolo.confidence.threshold=0.5
yolo.nms.threshold=0.4

# 살색 분석 설정
skin.ycrcb.min.cr=133
skin.ycrcb.max.cr=173
skin.threshold.25percent=0.25
skin.threshold.40percent=0.40

# 이미지 처리 설정
image.max.size=104857600
image.preprocessing.noise.reduction=true

# 성능 설정
performance.parallel.processing=true
performance.batch.size=10
```

### 로깅 설정 (logback.xml)

```xml
<!-- 로그 레벨 조정 -->
<logger name="com.skindetection" level="DEBUG" />
<logger name="com.skindetection.analyzer" level="INFO" />

<!-- 파일 출력 설정 -->
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/skin-detection-optimizer.log</file>
    <maxFileSize>10MB</maxFileSize>
    <maxHistory>30</maxHistory>
</appender>
```

## 📊 데이터 분석

### 엑셀 출력 컬럼

생성되는 Excel 파일에는 다음과 같은 데이터가 포함됩니다:

#### 기본 정보
- **ID**: 고유 식별자
- **분석일시**: 분석 수행 시간
- **파일명**: 이미지 파일명
- **파일크기**: 파일 크기 (KB)

#### YOLO 탐지 결과
- **탐지객체수**: 탐지된 객체 개수
- **최고신뢰도점수**: 가장 높은 신뢰도
- **여성가슴신뢰도**: EXPOSED_BREAST_F 신뢰도
- **남성가슴신뢰도**: EXPOSED_BREAST_M 신뢰도
- **엉덩이신뢰도**: EXPOSED_BUTTOCKS 신뢰도
- **여성성기신뢰도**: EXPOSED_GENITALIA_F 신뢰도
- **남성성기신뢰도**: EXPOSED_GENITALIA_M 신뢰도

#### 살색 분석 결과
- **전체살색비율**: 전체 이미지에서 살색이 차지하는 비율 (%)
- **Cr채널집중도**: Cr 채널의 색상 집중도 (%)
- **Cb채널집중도**: Cb 채널의 색상 집중도 (%)
- **25%임계값통과**: 25% 임계값 통과 여부
- **40%임계값통과**: 40% 임계값 통과 여부

#### 종합 평가
- **종합신뢰도점수**: 객체 탐지와 살색 분석을 종합한 점수
- **위험도등급**: HIGH, MEDIUM, LOW
- **필터권장사항**: BLOCK, REVIEW, ALLOW

### 통계 분석 시트

Excel 파일에는 다음과 같은 추가 시트가 포함됩니다:

1. **요약 통계**: 전체 데이터 개요
2. **모델별 분석**: 모델별 성능 비교
3. **임계값 분석**: 다양한 임계값에 대한 ROC 분석

## 🔧 문제 해결

### 자주 발생하는 문제

#### 1. 모델 파일을 찾을 수 없음

**오류 메시지**: `모델 파일을 찾을 수 없습니다: models/best_640n_0522.onnx`

**해결 방법**:
```bash
# models 디렉토리 확인 및 생성
mkdir -p models
# 모델 파일을 올바른 위치에 복사
cp /path/to/your/model.onnx models/best_640n_0522.onnx
```

#### 2. 메모리 부족 오류

**오류 메시지**: `OutOfMemoryError: Java heap space`

**해결 방법**:
```bash
# 힙 메모리 증가
java -Xmx8g -jar skin-detection-optimizer.jar

# 또는 application.properties에서 배치 크기 감소
performance.batch.size=5
```

#### 3. WebP 이미지 지원 오류

**오류 메시지**: `WebP 이미지 지원을 찾을 수 없습니다`

**해결 방법**:
```xml
<!-- pom.xml에 WebP 지원 라이브러리 추가 확인 -->
<dependency>
    <groupId>com.github.sejda</groupId>
    <artifactId>webp-imageio</artifactId>
    <version>0.2.6</version>
</dependency>
```

#### 4. OpenCV 로드 실패

**오류 메시지**: `OpenCV 로드 실패`

**해결 방법**:
- Windows: Visual C++ Redistributable 설치
- macOS: Homebrew로 OpenCV 설치
- Linux: 시스템 패키지 매니저로 OpenCV 설치

### 로그 확인

문제 해결을 위해 로그 파일을 확인하세요:

```bash
# 일반 로그
tail -f logs/skin-detection-optimizer.log

# 에러 로그
tail -f logs/skin-detection-optimizer-error.log

# 성능 로그
tail -f logs/skin-detection-optimizer-performance.log
```

### 성능 최적화

#### JVM 튜닝
```bash
# G1GC 사용 (Java 11+)
java -XX:+UseG1GC -Xmx8g -jar skin-detection-optimizer.jar

# 병렬 GC 사용
java -XX:+UseParallelGC -Xmx8g -jar skin-detection-optimizer.jar
```

#### 프로그램 설정
```properties
# 병렬 처리 활성화
performance.parallel.processing=true
performance.thread.pool.max.size=8

# 이미지 전처리 최적화
image.preprocessing.noise.reduction=false
image.preprocessing.contrast.enhancement=false
```

## 🤝 기여 방법

프로젝트에 기여해주신다면 대환영입니다!

### 개발 환경 설정

1. **Fork & Clone**
```bash
git clone https://github.com/your-username/skin-detection-optimizer.git
cd skin-detection-optimizer
```

2. **개발 브랜치 생성**
```bash
git checkout -b feature/new-feature
```

3. **의존성 설치**
```bash
mvn clean install
```

4. **테스트 실행**
```bash
mvn test
```

### 코딩 스타일

- **Java 코딩 컨벤션**: Oracle Java 코딩 컨벤션 준수
- **주석**: 모든 public 메서드에 JavaDoc 작성
- **테스트**: 새로운 기능에 대한 단위 테스트 작성
- **로깅**: SLF4J 사용, 적절한 로그 레벨 설정

### Pull Request 가이드라인

1. **Issue 연결**: 관련 Issue가 있다면 PR에 연결
2. **설명 작성**: 변경사항에 대한 상세한 설명
3. **테스트 결과**: 테스트 실행 결과 포함
4. **스크린샷**: UI 변경사항이 있다면 스크린샷 첨부

### 새로운 기능 제안

새로운 기능이나 개선사항이 있다면 Issue를 통해 제안해주세요:

- **기능 요청**: Feature Request 템플릿 사용
- **버그 리포트**: Bug Report 템플릿 사용
- **성능 개선**: Performance Issue 템플릿 사용

## 📄 라이선스

이 프로젝트는 [MIT 라이선스](LICENSE) 하에 배포됩니다.

```
MIT License

Copyright (c) 2024 Skin Detection Optimizer Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## 📞 지원 및 연락처

- **이슈 트래킹**: [GitHub Issues](https://github.com/your-username/skin-detection-optimizer/issues)
- **이메일**: dev@company.com
- **문서**: [Wiki](https://github.com/your-username/skin-detection-optimizer/wiki)

---

## 🙏 감사의 말

이 프로젝트는 다음 오픈소스 라이브러리들의 도움을 받아 개발되었습니다:

- [ONNX Runtime](https://onnxruntime.ai/) - AI 모델 추론
- [OpenCV](https://opencv.org/) - 컴퓨터 비전
- [Apache POI](https://poi.apache.org/) - Excel 파일 처리
- [WebP ImageIO](https://github.com/sejda-pdf/webp-imageio) - WebP 이미지 지원

---

**Happy Coding! 🎉**