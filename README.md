# 🍞 SmartFridge – 냉장고 속 식재료 관리 애플리케이션

**기간:** 2024년 9월 - 2024년 12월  
**캡스톤 프로젝트:** 3학년 2학기 캡스톤I, 성적 A+

---

## 💡 프로젝트 개요

SmartFridge는 요리 초보자와 1인 가구를 위해 식재료 관리와 요리 과정을 간편하게 도와주는 Android 기반 모바일 애플리케이션입니다.  

- 카메라 인식과 AI 모델을 활용하여 식재료를 자동으로 등록  
- 저장된 식재료를 기반으로 맞춤형 레시피 추천  
- 유통기한 관리 및 재료 중복 구매 방지  

---

## ✨ 핵심 기능

### 1. AI 기반 식재료 인식 및 저장
- 모바일 기기로 식재료 촬영 → AI가 자동 인식  
- 인식된 식재료명과 유통기한, 카테고리 등 부가 정보 저장  
- 실시간으로 냉장고 재고 확인 가능  

### 2. 식재료 관리
- 카테고리별 보유 식재료 확인  
- 유통기한 지정 기능으로 식재료 폐기 방지  
- 냉장고를 열지 않고도 앱에서 실시간 재고 확인 가능  

### 3. 레시피 추천 및 관리
- 저장된 식재료를 기반으로 레시피 자동 추천  
- 사용자가 직접 레시피 추가/저장 가능  
- AI 인식 결과와 레시피 재료 자동 매핑  

---

## 🛠 기술 스택

- **프로그래밍 언어:** Kotlin, Java  
- **Android 개발:** Android Studio, RecyclerView, Fragment, 모듈형 구조  
- **AI 모델:** TensorFlow Lite EfficientDet Lite0 (객체 인식)  
- **데이터베이스:** Firebase Firestore (NoSQL), 실시간 동기화  
- **인증:** Firebase Authentication  
- **기타 API:** Papago Translation API (영어 → 한국어 번역)  

---

## 📂 프로젝트 구조 (주요 코드)

```
SmartFridge/
├─ starterRealReal/ # 앱 코드
│ ├─ src/
│ │ ├─ main/java/com/... # 액티비티, 프래그먼트, 뷰모델
│ │ └─ res/ # 레이아웃, 이미지, 아이콘
│ └─ AndroidManifest.xml
├─ build.gradle
├─ settings.gradle
└─ README.md
```

---

## 🚀 설치 및 실행 방법

1. 저장소 클론
```bash
git clone https://github.com/cyoung-cy/SmartFridge.git
Android Studio에서 starterRealReal 폴더 열기
```

필요한 SDK 설치 후, 앱 실행 (Run)

---

## 👩‍💻 팀 및 역할
- 앱 프론트엔드 1명
- 앱 백엔드 1명
- TensorFlow Lite AI 모델 학습 1명
- 문서 작업 1명

## 🖊️ 학습/성과
- Android Studio 기반 모듈형 앱 설계 및 구현
- EfficientDet Lite0 모델 전이 학습(Fine-tuning) 후 TFLite 변환
- Firebase Firestore 활용, 컬렉션-도큐먼트 구조 기반 데이터 관리
- AI 모델 결과를 Papago Translation API로 실시간 한국어 변환
- 회원가입/로그인 기능 구현 및 인증 로직 연동
- end-to-end AI 파이프라인 구현 및 UX 고려

## 📈 향후 개선 사항
- 영상 인식 기반 식재료 등록
- 푸시 알림으로 유통기한 임박 안내
- 클라우드 DB 연동 및 다중 기기 동기화
- UI/UX 개선 및 기능 확장


