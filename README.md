# 말해요 영어 (English Speaking)

가족이 함께 쓰는 AI 영어 말하기 앱 (안드로이드)

- 🗣️ **AI 튜터**: 롤플레이, 일상, 비즈니스, OPIc, 고급, 어린이 상황별 대화. 채팅 모드와 음성 모드를 지원하고, 틀린 표현을 고쳐 주며 대화가 끝나면 리포트를 보여줍니다.
- 📚 **단어장**: 카드 학습, 퀴즈, 발음 채점, 간격 반복 복습, AI로 주제별 단어장 생성
- 🧩 **패턴**: 대화·뉴스에서 나온 좋은 문장을 `I'm looking for ~` 같은 패턴으로 저장하고 말하기 복습
- 🎧 **뉴스 듣기**: 영어 뉴스를 문장별로 듣기(원문 또는 쉬운 영어). 속도·반복·자막 숨기기, 쉐도잉, 핵심 단어와 질문 제공

## 설치

1. GitHub의 **Actions** 탭에서 가장 최근 성공한 실행을 엽니다. 아래 **english-speaking-apk**를 내려받아 압축을 풀면 APK가 나옵니다.
   `v0.1.0`처럼 태그를 붙이면 **Releases**에도 APK가 첨부됩니다.
2. 폰에서 APK를 엽니다. "출처를 알 수 없는 앱 설치"를 허용하고 설치합니다.
3. 업데이트할 때 기존 데이터를 유지하려면 모든 빌드를 같은 키로 서명해야 합니다. 아래 "서명 키" 설정을 참고하세요.

## AI 연결 (처음 한 번)

앱의 **홈 → ⚙️ 설정 → AI 연결**에서 아래 중 하나 이상의 API 키를 입력합니다.

| AI | 키 발급 | 비고 |
|---|---|---|
| Claude | console.anthropic.com → API Keys | 기본 모델 `claude-sonnet-5-5` |
| ChatGPT | platform.openai.com → API keys | |
| Gemini | aistudio.google.com → Get API key | 무료 사용량이 있음 |

- Claude Pro, ChatGPT Plus 같은 **구독 계정은 앱에 연결할 수 없습니다.** API 키는 구독과 별도이며, 쓴 만큼 요금이 나옵니다.
- **연결 테스트 · 모델 불러오기**를 누르면 사용 가능한 모델 목록이 나오고, 목록에서 고를 수 있습니다.
- 키는 그 폰 안에만 암호화되어 저장되고, 백업 파일에도 들어가지 않습니다.
- AI 없이도 단어장, 복습, 뉴스 원문 듣기는 쓸 수 있습니다.

## 가족과 공유

**설정 → 백업 · 가족과 공유 → 내보내기**로 파일을 만들어 가족에게 보냅니다. 받은 사람은 **가져오기**를 누르면 단어장, 패턴, 내가 만든 시나리오, 뉴스 피드가 합쳐집니다.

## 개발

```bash
./gradlew testDebugUnitTest   # 학습 로직 + 화면 흐름 테스트 (Robolectric)
./gradlew assembleRelease     # app/build/outputs/apk/release/app-release.apk
```

- 계획과 진행 현황: [PLAN.md](PLAN.md)
- 편의성·전문성·접근성 검토: [docs/REVIEW.md](docs/REVIEW.md)

### 서명 키 (업데이트 설치용)

서명 키는 저장소에 올리지 않습니다. GitHub 저장소의 **Settings → Secrets and variables → Actions**에 아래 두 값을 등록하면, Actions가 만드는 APK가 항상 같은 키로 서명됩니다.

| 이름 | 값 |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w0 family.jks` 결과 |
| `KEYSTORE_PASSWORD` | 키스토어 비밀번호 |

키 만들기: `keytool -genkeypair -keystore family.jks -alias family -keyalg RSA -keysize 2048 -validity 36500`

로컬에서는 `keystore/family.jks`가 있으면 그 키로 서명합니다. 비밀번호는 환경변수 `KEYSTORE_PASSWORD`로 지정합니다.
Secrets가 없으면 debug 키로 서명합니다. 이 경우 설치는 되지만 빌드마다 서명이 달라져, 업데이트하려면 지우고 다시 설치해야 합니다(설치 전에 백업하세요).
