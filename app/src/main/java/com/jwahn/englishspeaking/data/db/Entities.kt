package com.jwahn.englishspeaking.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 단어장 챕터. source: builtin / ai / import / news / tutor */
@Entity(tableName = "chapter")
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val source: String = "builtin",
    val createdAt: Long = System.currentTimeMillis(),
    /** 0 = 왕초보 과정, 1 = 기초, 2 = 일반 */
    val level: Int = 2,
    val emoji: String = "",
)

/**
 * 단어 + 라이트너 SRS 상태.
 * box 0 = 아직 학습 안 함, 1..5 = 라이트너 상자.
 */
@Entity(
    tableName = "word",
    indices = [Index("chapterId"), Index("nextDue")],
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chapterId: Long,
    val text: String,
    val ipa: String = "",
    val meaningKo: String,
    val exampleEn: String = "",
    val exampleKo: String = "",
    val bookmarked: Boolean = false,
    val box: Int = 0,
    val nextDue: Long = 0,
    val correct: Int = 0,
    val wrong: Int = 0,
    val bestScore: Int = 0,
    val emoji: String = "",
)

/** 재사용 가능한 문장 패턴. 예: "I'm thinking of ~ing" */
@Entity(tableName = "pattern", indices = [Index(value = ["template"], unique = true)])
data class PatternEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val template: String,
    val meaningKo: String,
    val category: String = "일반",
    val source: String = "ai",
    val box: Int = 0,
    val nextDue: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "pattern_example", indices = [Index("patternId")])
data class PatternExampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patternId: Long,
    val en: String,
    val ko: String = "",
)

/** AI 튜터 시나리오. category: ROLEPLAY / DAILY / BUSINESS / OPIC / ADVANCED / CUSTOM */
@Entity(tableName = "scenario")
data class ScenarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val title: String,
    val emoji: String = "💬",
    val persona: String,
    val goal: String,
    val missions: String = "", // 줄바꿈 구분
    val builtin: Boolean = true,
)

@Entity(tableName = "conversation")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scenarioId: Long,
    val title: String,
    val startedAt: Long = System.currentTimeMillis(),
    val reportJson: String? = null,
)

@Entity(tableName = "message", indices = [Index("conversationId")])
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val role: String, // user / assistant
    val text: String,
    val ko: String = "",
    val correctionJson: String? = null,
    val extrasJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/** AI 튜터에서 사용자가 실제로 말한 고유 단어 (레벨 산정용) */
@Entity(tableName = "used_word")
data class UsedWordEntity(
    @PrimaryKey val word: String,
    val firstUsedAt: Long = System.currentTimeMillis(),
)

/** 날짜별 학습량 (스트릭·통계) date = yyyy-MM-dd */
@Entity(tableName = "daily_stat")
data class DailyStatEntity(
    @PrimaryKey val date: String,
    val reviews: Int = 0,
    val sentences: Int = 0,
    val listened: Int = 0,
)

@Entity(tableName = "news_feed")
data class NewsFeedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val enabled: Boolean = true,
)

/** 뉴스 듣기용으로 가공된 기사 (재청취용 캐시) */
@Entity(tableName = "news_lesson")
data class NewsLessonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val mode: String, // ORIGINAL / EASY
    val json: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastPosition: Int = 0,
)
