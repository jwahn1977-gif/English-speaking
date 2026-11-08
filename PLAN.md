# English Speaking — AI 영어 말하기·단어 학습 앱 개발 계획

> 목표: 안드로이드 APK로 설치하는 **AI 튜터 기반 영어 말하기 + 단어 암기 앱**
> AI 튜터와 카테고리별 대화 연습, 단어 연습장, LLM(Claude / Gemini / GPT) 실시간 생성, 생성 결과를 **패턴 단위**로 저장해 복습.

---

## 1. 참고 화면 분석 → 우리 앱에 가져올 기능

첨부 스크린샷(야나두 틈새단어, 듀오링고, 스픽형 프리톡, AI 원어민톡)에서 **기능 아이디어만** 추립니다.
이름, 캐릭터, 이미지, 콘텐츠는 가져오지 않고 자체 브랜드와 마스코트로 만듭니다.

| 참고 화면 | 핵심 기능 | 우리 앱 반영 |
|---|---|---|
| 단어 카드 (tongue / tʌŋ / 혀) | 단어 + 발음기호 + 이미지 + 뜻 + 예문, 듣기·말하기 버튼 | **단어 카드**: TTS 듣기, STT 따라 말하기, 북마크 |
| 에빙하우스 망각곡선 / 라이트너 | 간격 반복 암기 | **SRS 엔진**(라이트너 5단계 상자) |
| 챕터별 암기율·암기강도·최고점수 | 챕터 진도 시각화 | **챕터 대시보드**, 통합 복습, 북마크 학습 |
| AI 튜터 레벨 (Lv1 단어만 → Lv5…) | 사용 어휘 수로 성장 단계 | **레벨 시스템**: 대화에서 실제 사용한 고유 단어 수 기준 |
| 프리톡: 롤플레이 / 비즈니스 / OPIc / 고급 | 주제별 시나리오 대화 | **카테고리별 AI 튜터** |
| 나만의 시나리오 만들기 / 커뮤니티 | 사용자 정의 시나리오 | **커스텀 시나리오**(1차는 로컬, 공유는 후순위) |
| 강의 상세(영상 → AI 트레이닝) | 학습 경로 | **유닛 경로**: 패턴 학습 → AI 트레이닝 → 미션 |
| 달성률 / 진도율 / 미션 | 진척 지표 | **3가지 지표**를 정의를 명확히 해서 표시 |
| AI 원어민톡 채팅 / 음성 모드 | 페르소나 대화, 텍스트·음성 전환 | **채팅 모드 + 음성 모드** 토글 |
| 스트릭, 젬, 에너지 | 게이미피케이션 | **스트릭 + 일일 목표**(과금 요소는 없음) |

---

## 2. 기술 스택

| 영역 | 선택 | 이유 |
|---|---|---|
| 언어 / UI | **Kotlin + Jetpack Compose (Material 3)** | 네이티브 APK, 최신 표준 |
| 구조 | MVVM + Clean Architecture(`data` / `domain` / `ui`), 단일 Activity + Navigation Compose | 기능 단위 확장이 쉬움 |
| DI | Hilt | |
| 로컬 DB | **Room** (+ FTS로 단어/패턴 검색) | 단어·패턴·대화 저장, 오프라인 동작 |
| 설정 | DataStore | |
| 비밀값 | **Android Keystore + EncryptedSharedPreferences** | API 키/토큰 암호화 보관 |
| 네트워크 | OkHttp + Retrofit, **SSE 스트리밍** | LLM 응답을 실시간으로 표시 |
| 음성 | Android `SpeechRecognizer`(STT), `TextToSpeech`(TTS) | 무료, 기기 내장. 이후 고품질 TTS 옵션 추가 |
| 백그라운드 | WorkManager | 복습 알림, 스트릭 리마인더 |
| 빌드/배포 | Gradle (AGP) + **GitHub Actions에서 APK 자동 빌드** | 푸시하면 APK가 Artifact로 생성됨 |
| 최소 사양 | minSdk 26 (Android 8.0), 최신 targetSdk | |

---

## 3. LLM 연동 (Claude / Gemini / GPT)

### 3.1 "로그인" 방식 — 현실적인 선택지

| 방식 | 가능 여부 | 비고 |
|---|---|---|
| Claude Pro / ChatGPT Plus 같은 **구독 계정으로 로그인**해서 API처럼 사용 | ❌ 서드파티 앱에는 일반적으로 제공되지 않음 | 구독과 API는 별도 상품 |
| **API 키 입력 (BYOK, Bring Your Own Key)** | ✅ 세 회사 모두 가능 | **1차 기본안**. 키는 기기 Keystore에 암호화 저장 |
| Google 계정 OAuth로 Gemini API 호출 | 🔶 검토 필요 | Google Sign-In 연동 가능성을 M2에서 PoC |
| 자체 백엔드 프록시(우리 서버가 키 보관) | ✅ 가능 | 여러 사용자 배포 시 2차 옵션 |

→ **설정 > AI 연결** 화면에서 제공사별 키 등록·연결 테스트·기본 모델 선택을 하도록 구성합니다.
모델 ID는 하드코딩하지 않고 설정값으로 둡니다(신모델이 나오면 교체).
기본 추천값: Claude는 `claude-sonnet-5-5`(대화), `claude-haiku-5-5`(저비용 생성)입니다.

### 3.2 공통 인터페이스

```kotlin
interface LlmProvider {
    val id: ProviderId               // CLAUDE, GEMINI, OPENAI
    fun streamChat(req: ChatRequest): Flow<ChatChunk>              // 실시간 대화
    suspend fun generateStructured(req: GenRequest): JsonObject    // 단어/패턴 생성(JSON)
}
```

- 구현체: `ClaudeProvider`(Messages API), `OpenAiProvider`, `GeminiProvider`
- `LlmRouter`: 기능별로 제공사·모델을 지정(예: 대화는 Claude, 단어 생성은 Gemini)하고 실패 시 다른 제공사로 넘김
- 프롬프트 템플릿은 `assets/prompts/*.md`로 분리해서 코드 변경 없이 수정

### 3.3 AI 튜터 프롬프트 구성
- **시스템 프롬프트** = 튜터 페르소나 + 시나리오 + 사용자 레벨(어휘 수준 제한) + 교정 규칙
- 매 턴의 응답은 대화문과 피드백을 분리한 구조로 받습니다.
  ```json
  { "reply": "...", "correction": {"original":"...","better":"...","why_ko":"..."},
    "new_patterns": [...], "new_words": [...] }
  ```
- `new_patterns`와 `new_words`는 사용자가 ⭐로 저장할 수 있게 노출합니다(→ 4장).

---

## 4. "저장된 건 패턴으로 구성" — 데이터 설계

AI가 생성한 문장, 대화, 교정 결과를 그대로 쌓지 않고 **재사용 가능한 패턴**으로 정규화해 저장합니다.

### 4.1 패턴 예시
```json
{
  "pattern": "I'm thinking of ~ing",
  "meaning_ko": "~할까 생각 중이야",
  "slots": [{"name":"verb-ing","examples":["moving","quitting my job","learning Spanish"]}],
  "examples": [
    {"en":"I'm thinking of moving to Busan.","ko":"부산으로 이사할까 생각 중이야."}
  ],
  "category": "일상/계획", "level": 2,
  "source": {"type":"ai_tutor","conversation_id":123}
}
```

### 4.2 Room 스키마 (초안)

| 테이블 | 주요 컬럼 |
|---|---|
| `word` | id, text, ipa, meanings_ko, example_en/ko, image_uri, chapter_id, source |
| `pattern` | id, template, meaning_ko, slots_json, category, level, source_type, source_ref |
| `pattern_example` | id, pattern_id, en, ko, audio_uri |
| `pattern_word` | pattern_id, word_id (N:M) |
| `review_card` | id, item_type(WORD/PATTERN), item_id, box(1–5), next_due, correct/wrong 횟수, last_score |
| `chapter` / `unit` | 단어장 챕터, 학습 유닛 |
| `scenario` | id, category(ROLEPLAY/BUSINESS/OPIC/ADVANCED/CUSTOM), title, persona, goal, system_prompt |
| `conversation` / `message` | 대화 기록, 교정 결과, 사용한 단어 |
| `daily_stat` | date, minutes, words_reviewed, sentences_spoken, streak |
| `provider_config` | provider, model, enabled (키 자체는 Keystore에 별도 저장) |

### 4.3 저장 흐름
1. AI 대화나 생성 결과에서 `new_patterns` / `new_words` 후보가 나옴
2. 사용자가 ⭐ 저장 → 기존 패턴과 **중복 검사**(템플릿 정규화 + 유사도) → 있으면 예문만 추가
3. 저장과 동시에 `review_card` 생성 → SRS 복습에 자동 편입
4. **패턴 라이브러리** 화면: 카테고리·레벨·출처별 필터, 패턴 하나로 "슬롯 바꿔 말하기" 연습

---

## 5. 기능 모듈

### 5.1 단어 연습장
- **챕터 목록**: 암기율, 암기강도, 최고점수 원형 게이지
- **단어 카드**: 단어 / 발음기호 / 뜻 / 예문 / 이미지(선택), 🔊 TTS, 🎤 따라 말하기(STT 결과와 비교해 점수)
- 학습 모드: 카드 넘기기 → 뜻 고르기 퀴즈 → 듣고 쓰기 → 말하기
- **AI 단어 세트 생성**: "비즈니스 이메일 단어 20개, Lv2" → JSON 생성 → 미리보기 → 저장
- 통합 복습, 북마크 학습, CSV 가져오기/내보내기

### 5.2 SRS(간격 반복) 엔진
- 라이트너 5상자, 복습 간격 1 / 2 / 4 / 7 / 15일(설정에서 변경 가능)
- 정답 → 상자 +1, 오답 → 상자 1로 이동
- 지표 정의(화면에 정의를 같이 표시해서 숫자를 오해하지 않게 함):
  - **암기율** = 상자 3 이상인 카드 / 전체 카드
  - **암기강도** = 평균 상자 번호 / 5
  - **진도율** = 한 번 이상 학습한 카드 / 전체 카드

### 5.3 AI 튜터 (카테고리별 연습)
| 카테고리 | 예시 시나리오 |
|---|---|
| 롤플레이 | 카페 주문, 새 친구 사귀기, 매장 쇼핑, 생일 파티 |
| 비즈니스 | 면접(경력/업무 스타일/가상 상황), 회의, 1:1 면담, 이메일 |
| OPIc 연습 | 자기소개, 묘사, 롤플레이, 돌발 질문 + 등급 추정 피드백 |
| 고급 | 토론, 뉴스 요약, 의견 말하기 |
| 나만의 시나리오 | 상황, 상대 역할, 목표를 입력하면 시스템 프롬프트 자동 생성 |

- **채팅 모드 / 음성 모드** 토글. 음성 모드는 STT → LLM 스트리밍 → TTS 순서로 처리
- 막힐 때 **힌트** 버튼(한국어로 말하면 영어 표현 제안)
- 대화가 끝나면 **리포트**: 교정 목록, 새 패턴, 사용 어휘 수, 말한 문장 수 → 패턴 저장으로 연결
- 시나리오별 **미션**(예: "가격을 물어보기", "할인 요청하기") 달성 체크

### 5.4 레벨·동기부여
- 레벨 기준: 대화에서 **실제로 사용한 고유 단어 수**(100 / 300 / 600 / 1,200 / 2,500…)
- 자체 마스코트가 레벨마다 성장하는 일러스트
- 스트릭, 일일 목표(분 / 문장 수), WorkManager 복습 알림

---

## 6. 화면 구성 (Navigation)

```
하단 탭: [홈] [AI 튜터] [단어장] [패턴] [프로필]

홈 ─ 오늘의 복습 N개 / 이어하기 / 스트릭 / 추천 시나리오
AI 튜터 ─ 카테고리 탭 → 시나리오 카드 → 대화(채팅|음성) → 리포트
단어장 ─ 챕터 목록 → 카드 학습 / 퀴즈 / 통합 복습 / 북마크 / AI 생성
패턴 ─ 라이브러리(필터·검색) → 패턴 상세 → 슬롯 바꿔 말하기 연습
프로필 ─ 레벨·통계 / 설정 / AI 연결(키·모델) / 백업
```

---

## 7. 프로젝트 구조

```
app/
 ├─ data/
 │   ├─ db/            Room entities, DAO, migrations
 │   ├─ llm/           LlmProvider, Claude/OpenAi/Gemini 구현, LlmRouter, prompts
 │   ├─ speech/        SttManager, TtsManager
 │   └─ repo/          WordRepo, PatternRepo, ConversationRepo, ReviewRepo
 ├─ domain/            UseCases (GeneratePatterns, ScheduleReview, ScoreSpeech …), SRS 엔진
 ├─ ui/
 │   ├─ home/ tutor/ vocab/ pattern/ profile/ settings/
 │   └─ components/    카드, 게이지, 마이크 버튼 등
 └─ assets/prompts/    튜터·생성용 프롬프트 템플릿
.github/workflows/android.yml   APK 자동 빌드
```

---

## 8. 마일스톤

| 단계 | 기간(예상) | 산출물 | 완료 기준 |
|---|---|---|---|
| **M0 기반** | 1주 | Compose 프로젝트, Hilt, Room, 탭 내비게이션, GitHub Actions APK 빌드 | Actions에서 받은 debug APK가 폰에 설치·실행됨 |
| **M1 단어 연습장** | 2주 | 챕터, 단어 카드, TTS/STT, 라이트너 SRS, 지표, 기본 단어 세트 | 오늘 복습 → 상자 이동 → 지표 갱신 |
| **M2 LLM 연동** | 1.5주 | AI 연결 설정(키 암호화), Claude/GPT/Gemini 스트리밍, 라우터 | 세 제공사 모두 연결 테스트 통과 |
| **M3 AI 튜터** | 2주 | 카테고리·시나리오, 채팅 모드, 교정 JSON, 리포트, 커스텀 시나리오 | 시나리오 1회 완주 → 리포트 생성 |
| **M4 패턴 시스템** | 1.5주 | 패턴 추출·중복 병합·라이브러리·슬롯 연습, SRS 편입 | 대화에서 저장한 패턴이 다음 날 복습에 나옴 |
| **M5 음성 모드** | 1.5주 | STT → LLM → TTS 파이프라인, 발음 점수, 끼어들기 처리 | 손 안 대고 5턴 대화 가능 |
| **M6 레벨·동기부여** | 1주 | 레벨, 스트릭, 알림, 통계 화면, 마스코트 | |
| **M7 마감** | 1주 | 백업/복원(JSON), 오류 처리, 서명된 release APK | 서명 APK 배포 |

총 약 **11~12주**(1인 기준). M0 → M1 → M2 → M3 순서로 진행하면 4~5주 차부터 실제 대화 연습이 가능한 버전을 쓸 수 있습니다.

---

## 9. APK 빌드·배포

- `.github/workflows/android.yml`: push할 때마다 `./gradlew assembleDebug` → APK를 Artifact로 업로드
- Release: 서명 키스토어를 GitHub Secrets에 base64로 저장 → 태그 push 시 `assembleRelease` → GitHub Release에 APK 첨부
- 설치: 폰에서 "알 수 없는 앱 설치 허용" 후 APK 설치(Play 스토어 등록은 선택)

---

## 10. 리스크와 대응

| 리스크 | 대응 |
|---|---|
| API 비용 증가 | 기능별로 저가 모델 지정, 응답 길이 제한, 생성 결과 캐시, 월 사용량 표시 |
| 구독 계정 로그인 불가 | BYOK를 기본으로 하고, Gemini OAuth는 PoC 후 결정 |
| 한국어 억양에 대한 STT 인식률 | 인식 실패 시 텍스트 보정 입력 허용, 발음 점수는 참고용으로만 표시 |
| API 키 유출 | Keystore 암호화, 로그·백업에서 키 제외 |
| 콘텐츠 저작권 | 참고 앱의 단어장·이미지·캐릭터·강의는 사용하지 않음. 자체 또는 AI 생성 콘텐츠만 사용 |
| 오프라인 | 단어장, SRS, 패턴 복습은 오프라인 동작. AI 기능만 온라인 필요 |

---

## 11. 확장 아이디어 (후순위)

- 커뮤니티 시나리오 공유(백엔드 필요)
- 고품질 TTS/STT(클라우드), 발음 음소 단위 평가
- **팀 학습 리포트**: 팀원별 학습 시간·복습 완료율 같은 **노력 지표**와 대화 레벨·교정 감소율 같은 **성과 지표**를 분리하고, 절대 점수 대신 **시작 레벨 대비 성장률**로 비교해 출발점이 다른 팀원도 공정하게 볼 수 있게 설계

---

## 12. 바로 다음 작업

1. M0: Android 프로젝트 골격 생성 + GitHub Actions APK 빌드
2. Room 스키마(4.2) 확정 및 마이그레이션 v1
3. 기본 단어 세트(자체 제작 300단어, 5챕터) 준비
4. Claude Provider부터 연동 → GPT → Gemini 순서
