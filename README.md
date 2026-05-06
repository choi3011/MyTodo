# MyTodo

> 모바일과 데스크톱이 같은 데이터를 실시간으로 동기화하는 Todo 관리 앱

**한국어** &nbsp;|&nbsp; [English](README.en.md)

안드로이드와 데스크톱에서 모두 동작하는 Todo 관리 앱. 두 클라이언트가 같은 Firebase 백엔드를 공유해서, 한 쪽에서 추가한 todo가 다른 쪽에도 거의 실시간으로 반영됨. UI는 Material You의 기본 룩을 피하고 보라-마젠타 그라디언트와 hero 타이포그래피로 직접 디자인했음.

## 화면

> 스크린샷은 `docs/screenshots/` 디렉토리에 추가 예정. 모바일과 데스크톱이 동일한 시각 정체성을 공유하는 모습 비교.

## 주요 기능

- **4개의 시간 스코프** — Today / Week / Month / Year, 각각 고유 색상 액센트
- **우선순위 4단계** — None / Low / Medium / High, 최신순/우선순위순 토글
- **시간 범위** — `HH:mm ~ HH:mm` 24시간 형식 직접 입력
- **캘린더 점** — todo가 있는 날짜를 시각적으로 표시
- **Google 로그인** — 모바일은 Credential Manager, 데스크톱은 OAuth desktop flow + PKCE
- **Cross-platform 동기화** — Android ↔ Desktop이 같은 Firestore 문서 트리 공유
- **세션 영속화** — refresh token 로컬 저장, 재시작 시 자동 로그인

## 기술 스택

| 영역 | 사용 기술 |
| --- | --- |
| Mobile | Kotlin 2.0, Jetpack Compose Material 3, AGP 9, minSdk 24 |
| Desktop | Compose Multiplatform 1.7, Kotlin/JVM, JDK 17+ |
| Auth | Firebase Auth, Google OAuth 2.0 (Credential Manager + 자체 PKCE 구현) |
| Storage | Firebase Firestore (모바일은 snapshot listener / 데스크톱은 REST `runQuery`) |
| Build | Gradle, jpackage, WiX (Windows MSI) |

## 아키텍처

```
┌──────────────┐     ┌─────────────────┐     ┌──────────────┐
│  Android     │     │  Firestore      │     │  Compose     │
│  (mobile)    │◄───►│  users/{uid}/   │◄───►│  Desktop     │
│              │     │  todos/{id}     │     │  (JVM)       │
└──────────────┘     └─────────────────┘     └──────────────┘
   snapshot              security rules:        REST runQuery
   listener              auth.uid == uid        + 15s polling
   (push, gRPC)                                 + per-scope cache
```

두 클라이언트가 동일한 Google 계정으로 로그인하면 같은 Firebase UID를 받고, 그 UID 하위의 `todos` 컬렉션을 공유함. 모바일은 SDK가 제공하는 snapshot listener(gRPC stream)로 push 기반 실시간 sync. 데스크톱은 REST API + 폴링 + 캐시 조합으로 push에 가까운 효율을 냄.

## 빌드 & 실행

### 사전 요구사항

- Android Studio (모바일 앱)
- JDK 17 이상 (데스크톱 — JBR(JetBrains Runtime)은 `jpackage`가 빠져있어서 패키징에 부적합. Eclipse Temurin 또는 Microsoft OpenJDK 권장)
- 본인의 Firebase 프로젝트 (Authentication, Firestore Database 활성화)

### Firebase 설정

1. Firebase Console에서 새 프로젝트 생성
2. **Authentication** → 로그인 방법 → Google 활성화
3. **Firestore Database** → 시작하기 → 보안 규칙 다음으로 교체:
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{uid}/todos/{document=**} {
         allow read, write: if request.auth != null && request.auth.uid == uid;
       }
     }
   }
   ```
4. **Project settings** → Android 앱 추가 → `google-services.json` 다운로드 → `app/`에 복사
5. **Project settings** → 웹 client_id 확인 (Authentication → Sign-in method → Google에서 자동 생성됨). 이 ID는 모바일 Credential Manager에 사용됨.

### Android 앱

```
Android Studio에서 프로젝트 열기 → 'app' run configuration 실행
```

minSdk 24부터 동작.

### 데스크톱 앱

1. **Google Cloud Console에서 Desktop OAuth 클라이언트 생성**
   - https://console.cloud.google.com/apis/credentials → CREATE CREDENTIALS → OAuth client ID
   - Application type: **Desktop app**
   - Client ID + Client Secret 받음

2. **`OAuthConfig.kt` 수정** (`desktop/src/main/kotlin/com/example/mytodo/desktop/auth/OAuthConfig.kt`):
   - `GOOGLE_CLIENT_ID`를 새로 받은 Desktop OAuth client ID로 교체
   - `FIREBASE_API_KEY`를 본인 Firebase 프로젝트의 web API key로 교체
   - Firestore project ID도 `FirestoreClient.kt`에서 본인 프로젝트로 교체

3. **`local.properties`에 client_secret 추가**:
   ```
   google.oauth.client_secret=YOUR_DESKTOP_CLIENT_SECRET
   ```
   이 파일은 gitignore돼있어 git에 들어가지 않음.

4. **개발 실행**:
   ```
   ./gradlew :desktop:run
   ```

5. **Windows 인스톨러 빌드** (전체 JDK 17+ 필요):
   ```
   ./gradlew :desktop:packageMsi
   ```
   결과: `desktop/build/compose/binaries/main/msi/MyTodo-1.0.0.msi`. 더블클릭으로 설치하면 시작 메뉴 + 바탕화면 바로가기 자동 생성.

## 프로젝트 구조

```
.
├── app/                                # Android module
│   └── src/main/
│       ├── java/com/example/mytodo/
│       │   ├── data/                   # Firestore + Auth repositories
│       │   ├── ui/                     # TodoScreen, LoginScreen, ViewModel
│       │   └── ui/components/          # TodoRow, AddTodoSheet, CalendarPicker, ...
│       └── res/                        # icons, themes, splash
├── desktop/                            # Compose Desktop module
│   ├── src/main/
│   │   ├── kotlin/com/example/mytodo/desktop/
│   │   │   ├── Main.kt                 # window + auth state branching
│   │   │   ├── auth/                   # OAuth desktop flow + Firebase token exchange
│   │   │   ├── data/                   # Firestore REST client + Todo repository
│   │   │   └── ui/                     # mirrors mobile components
│   │   └── resources/                  # icon.png, icon.ico
│   └── build-icon.ps1                  # PowerShell + GDI+ icon generator
├── gradle/libs.versions.toml           # version catalog
└── settings.gradle.kts                 # multi-module setup
```

## 설계 결정

### 모바일 우선 — 데스크톱은 같은 경험을 폰 폼 팩터로
데스크톱 창은 폰 비율(480×820)을 유지. desktop-native sidebar 패턴 대신 모바일 UI를 그대로 이식해서 시각적 정체성을 공유. UI 컴포넌트 90% 이상이 import 라인만 바꾸고 그대로 동작 (`androidx.compose.*` 네임스페이스 호환).

### 데스크톱 동기화는 REST 폴링 (모바일은 SDK push)
Firestore의 실시간 Listen 채널은 gRPC 양방향 스트리밍이라 JVM에서 직접 구현하기엔 의존성과 복잡도 모두 부담. 대신 REST `runQuery` 엔드포인트로 현재 보이는 scope + targetDate만 필터링한 쿼리를 15초 주기로 폴링. Per-`(scope, anchor)` 캐시와 1초 debounce를 추가해서:

- 같은 scope에 머무를 때만 폴링 (탭 빠르게 전환은 fetch 안 함)
- 한 번 가져온 scope는 재방문 시 캐시에서 즉시 표시 + 백그라운드 새로고침
- 창 minimize 시 폴링 자동 정지 (`WindowState.isMinimized`)

결과: todo가 5,000개 쌓여도 한 번 폴링 = 화면에 보이는 5~30개만 reads로 카운트. Firestore 무료 한도 안에서 안정적으로 운영 가능.

### 데스크톱 OAuth는 RFC 8252 표준 구현
Compose Desktop에는 Google sign-in 표준 SDK가 없음. RFC 8252 (OAuth 2.0 for Native Apps)에 따라 직접 구현:

1. **PKCE** — `code_verifier`(64바이트 랜덤) → SHA256 → base64url로 `code_challenge` 생성
2. **Loopback redirect** — `127.0.0.1` 임의 포트에 임시 `HttpServer` 띄움 (Java SE 표준)
3. **시스템 브라우저** — `java.awt.Desktop.browse()`로 Google authorization URL 열기
4. **Code 캡처** — redirect 콜백을 `awaitPointerEventScope` 패턴의 `Awaitable<String>`로 비동기 수신
5. **토큰 교환** — Google `oauth2.googleapis.com/token` POST → Google ID token
6. **Firebase 교환** — `identitytoolkit.googleapis.com:signInWithIdp`로 Google ID token을 Firebase ID token + refresh token으로 교환
7. **세션 영속화** — `~/.mytodo/session.json`에 저장, 재시작 시 자동 복원 (만료된 토큰은 refresh)

`securetoken.googleapis.com` refresh 엔드포인트로 1시간마다 자동 갱신. mutex로 동시 갱신 방지.

### 비밀 정보는 git에 안 들어감
- `.gitignore`로 `app/google-services.json`, `local.properties`, `client_secret_*.json`, 개발용 스크린샷/비디오 모두 제외
- Firestore 보안 규칙로 `request.auth.uid == uid`인 경우만 read/write 허용 (다른 사용자 데이터 접근 불가)
- OAuth client_secret은 spec상 진짜 secret이 아니지만 (desktop 앱과 함께 배포되는 식별자), public repo에 박지 않으려고 `local.properties`에서 런타임 로드

### 데스크톱 아이콘은 vector에서 PNG/ICO 자동 생성
모바일의 `ic_launcher_foreground.xml` (vector)에서 좌표를 가져와 PowerShell + GDI+로 16/32/48/64/128/256 사이즈 PNG를 일괄 렌더링하고, 그걸 묶어 멀티 사이즈 .ico 파일로 패킹. 모바일과 데스크톱이 픽셀 단위로 일치하는 아이콘을 가짐.

## 학습 정리

- **Compose Multiplatform 호환성** — Kotlin 2.0 + Compose 1.7 조합에서 모바일/데스크톱 UI 코드를 거의 그대로 공유 가능. `LocalHapticFeedback`, `LocalSoftwareKeyboardController` 등 일부 platform-specific API는 데스크톱에서 noop로 동작.
- **Firestore 과금 모델 직관 — list = N reads** — 폴링 + 무필터 list가 어떻게 비용을 폭증시키는지 측정. server-side 필터의 가치를 정량화.
- **OAuth 데스크톱 플로우** — RFC 8252의 loopback redirect + PKCE 패턴은 표준이지만 SDK 없이 직접 구현해보면 토큰의 흐름이 명확해짐.
- **세션 영속화 트레이드오프** — refresh token을 평문 파일로 저장하는 단순함 vs OS 키체인 통합의 보안. 개인 데스크톱 환경에선 user home 권한에 의존하는 게 합리적이라고 판단.
