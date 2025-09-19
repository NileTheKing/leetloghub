# LeetLog Hub

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/NileTheKing/leetloghub)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**LeetCode 풀이 기록 자동화 및 스마트 학습 대시보드**

LeetLog Hub는 사용자가 LeetCode에서 문제 풀이에 성공했을 때, 그 기록(소스 코드, 성능 지표, 문제 설명 등)을 사용자의 GitHub 레포지토리와 Notion 데이터베이스에 자동으로 아카이빙해주는 브라우저 확장 프로그램입니다.

단순 기록을 넘어, SRS(간격 반복 학습) 알고리즘에 기반한 다음 복습일을 계산하여 Notion에 함께 표시함으로써, 사용자가 자신의 학습 상태를 한눈에 파악하고 체계적인 복습 계획을 세울 수 있도록 돕습니다.

## 주요 기능

*   **자동 풀이 감지**: LeetCode에서 문제 제출 후 'Accepted' 결과를 받으면 자동으로 모든 관련 데이터를 수집합니다.
*   **GitHub 연동**: 지정된 GitHub 레포지토리에 문제 제목으로 된 폴더를 생성하고, 그 안에 소스 코드와 상세 설명이 담긴 README.md 파일을 자동으로 커밋합니다.
*   **Notion 연동**: 지정된 Notion 페이지 하위에 풀이 기록 데이터베이스를 생성하고, 문제 풀이 정보를 기반으로 SRS 알고리즘을 적용하여 다음 복습일, 학습 상태 등을 포함한 새로운 페이지를 생성하거나 업데이트합니다.
*   **안전한 인증**: GitHub/Notion 연동은 OAuth2 표준을 따르며, 백엔드 API와의 통신은 JWT(JSON Web Token)를 사용하여 안전하게 이루어집니다.

## 시작하기


## 1. 확장 프로그램 다운로드
이미지를 클릭하여 확장 프로그램 다운 페이지로 이동하여 다운로드 합니다.
--이미지--


### 2. 연동 설정

1.  설치된 확장 프로그램 아이콘을 클릭하여 팝업을 엽니다.
2.  'Login with GitHub' 버튼을 눌러 GitHub 계정을 인증합니다.
3.  자동으로 열리는 설정 페이지(`github-integration.html`)에서 GitHub 레포지토리와 Notion 연동을 순서대로 완료합니다.

### 기술 스택

*   **Backend**: Spring Boot, Spring Security, Spring Data JPA, PostgreSQL, JJWT, Docker-compose, RESTFUL API
*   **Frontend**: JS, HTML, CSS
*   **APIs**: GitHub REST API, Notion API

## 아키텍처

LeetLog Hub는 브라우저 확장 프로그램(Frontend)과 Spring Boot 서버(Backend)로 구성된 클라이언트-서버 아키텍처를 가집니다.

### 데이터 흐름

```
[LeetCode 페이지]
      |
 (1. 문제 제출 및 성공)
      |
      v
[interceptor.js (콘텐츠 스크립트)]
      |  |
      |  +--(2. /check/ API 응답 감시하여 'submission_id' 확보)
      |  |
      |  +--(3. GraphQL API에 직접 요청하여 전체 풀이 정보 획득)
      |  |
      |  +--(4. DOM에서 문제 제목, 설명 등 정적 정보 스크래핑)
      |
 (5. window.postMessage로 데이터 전송)
      |
      v
[bridge.js (콘텐츠 스크립트)]
      |
 (6. chrome.runtime.sendMessage로 데이터 전달)
      |
      v
[background.js (서비스 워커)]
      |
 (7. JWT와 함께 백엔드에 API 요청: POST /api/solves)
      |
      v
[Spring Boot 백엔드]
      |
      +-- (8. DB에 SolveLog, ProblemStatus 저장/업데이트)
      |
      +-- (9. Notion API 호출: DB 페이지 생성/업데이트)
      |
      +-- (10. GitHub API 호출: 폴더 및 파일 생성/업데이트)
      |
      v
[사용자 DB, Notion, GitHub]
```



## 기술적 핵심 사항

### 1. 풀이 성공 감지 방식 (2-Step Hybrid)

LeetCode의 동적인 UI 환경에서 안정적으로 풀이 성공을 감지하기 위해, 두 가지 API를 순차적으로 활용하는 하이브리드 방식을 채택했습니다.

1.  **1단계 (상태 폴링 감시)**: `interceptor.js`는 페이지의 `fetch` API를 재정의하여 모든 네트워크 요청을 감시합니다. 이 중 `/submissions/detail/{id}/check/` URL의 응답을 지속적으로 확인하여, 응답 내용에 `state: 'SUCCESS'`와 `status_msg: 'Accepted'`가 포함되면 풀이가 최종적으로 성공했다고 판단하고 `submission_id`를 추출합니다.

2.  **2단계 (GraphQL 상세 정보 요청)**: 1단계에서 얻은 `submission_id`를 변수로 사용하여, 소스 코드를 포함한 모든 상세 정보가 담겨있는 `/graphql` API에 직접 요청을 보냅니다. 이 요청에는 `x-csrftoken`과 `Referer` 헤더를 포함하여 인증을 통과합니다.

이 2단계 방식은 DOM 구조 변경에 영향을 받지 않으면서도, 필요한 모든 데이터를 가장 확실하게 얻을 수 있는 매우 안정적인 방법입니다.

### 2. MAIN 세계와 ISOLATED 세계 간의 통신

보안상의 이유로, 페이지의 `fetch`를 재정의하는 `interceptor.js`는 `MAIN` 세계에서 실행되어야 하며, 이 환경에서는 `chrome.*` 확장 프로그램 API를 사용할 수 없습니다. 이 문제를 해결하기 위해 `bridge.js`라는 중계 스크립트를 도입했습니다.

*   **`interceptor.js` (MAIN)**: 데이터 수집이 완료되면 `window.postMessage()`를 사용해 페이지 내부로 이벤트를 보냅니다.
*   **`bridge.js` (ISOLATED)**: `window.addEventListener()`로 이벤트를 수신한 뒤, `chrome.runtime.sendMessage()`를 사용해 백그라운드 스크립트로 안전하게 데이터를 전달하는 '다리' 역할을 수행합니다.


## 프로젝트 구조

```
.
├── frontend/              # 브라우저 확장 프로그램 (클라이언트)
│   ├── interceptor.js     # LeetCode 네트워크 요청 감시 및 데이터 수집
│   ├── bridge.js          # MAIN 세계와 ISOLATED 세계 간 통신 담당
│   ├── background.js      # 서비스 워커 (백엔드 API 호출 등 핵심 로직)
│   ├── popup.html         # 확장 프로그램 팝업 UI
│   └── github-integration.html # 설정 페이지 UI
│
└── src/main/java/com/leethublog/
    ├── config/            # Security, JWT, Web 설정
    │   ├── JwtTokenProvider.java
    │   ├── SecurityConfig.java
    │   └── WebConfig.java
    ├── controller/        # API 엔드포인트
    │   ├── SolveController.java
    │   └── api/ConfigurationController.java
    ├── domain/            # JPA 엔티티
    │   ├── Member.java
    │   ├── SolveLog.java
    │   └── ProblemStatus.java
    ├── service/           # 비즈니스 로직
    │   ├── SolveService.java
    │   ├── GithubService.java
    │   └── NotionService.java
    └── repository/        # Spring Data JPA 리포지토리
```

## 향후 개선 과제

*   **풀이 상태(SolveStatus) 선택 UI**: 현재는 풀이 상태가 'GOOD'으로 하드코딩되어 있습니다. 제출 성공 시 작은 팝업이나 UI를 주입하여 사용자가 직접 자신의 풀이 경험(PERFECT, BAD 등)을 선택할 수 있도록 개선이 필요합니다.
*   **GitHub 파일 업데이트**: 현재는 GitHub에 파일이 없을 때 새로 생성하는 로직만 구현되어 있습니다. 이미 파일이 존재할 경우 덮어쓰거나 업데이트하는 로직(기존 파일의 SHA 값을 이용)을 추가해야 합니다.
*   **에러 핸들링 고도화**: 네트워크 실패나 API 에러 발생 시, 재시도 로직이나 사용자에게 실패를 명확히 알려주는 UI 피드백을 강화할 수 있습니다.
