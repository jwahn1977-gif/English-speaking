package com.jwahn.englishspeaking.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import java.time.LocalDate

/** LLM 응답에서 JSON 객체를 안전하게 뽑아낸다. (```json 코드블록, 앞뒤 설명문 허용) */
object JsonText {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parseObject(text: String): JsonObject? {
        val cleaned = text.trim()
            .removePrefix("```json").removePrefix("```JSON").removePrefix("```")
            .removeSuffix("```").trim()
        runCatching { return json.parseToJsonElement(cleaned).jsonObject }
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { json.parseToJsonElement(cleaned.substring(start, end + 1)).jsonObject }.getOrNull()
    }
}

fun JsonObject.str(key: String): String =
    (this[key] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()

fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

fun JsonObject.arr(key: String): List<JsonElement> = (this[key] as? JsonArray).orEmpty()

fun JsonObject.objs(key: String): List<JsonObject> = arr(key).mapNotNull { it as? JsonObject }

fun JsonObject.ints(key: String): List<Int> =
    arr(key).mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.toIntOrNull() }

/** 라이트너 5상자 간격 반복 */
object Srs {
    const val MAX_BOX = 5
    private const val DAY = 24L * 60 * 60 * 1000

    /** 상자별 복습 간격(일): 1→1일, 2→2일, 3→4일, 4→7일, 5→15일 */
    val intervalsDays = intArrayOf(0, 1, 2, 4, 7, 15)

    data class Result(val box: Int, val nextDue: Long)

    fun answer(box: Int, correct: Boolean, now: Long = System.currentTimeMillis()): Result =
        if (correct) {
            val nb = (box + 1).coerceIn(1, MAX_BOX)
            Result(nb, now + intervalsDays[nb] * DAY)
        } else {
            // 틀리면 1번 상자로, 다음 날 다시
            Result(1, now + intervalsDays[1] * DAY)
        }
}

/** 말하기 채점: 목표 문장과 인식 결과의 단어 단위 LCS 일치율 (0~100) */
object SpeechScore {
    fun normalize(s: String): List<String> =
        s.lowercase()
            .replace("’", "'")
            .replace(Regex("[^a-z0-9' ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

    fun score(target: String, spoken: String): Int {
        val t = normalize(target)
        val s = normalize(spoken)
        if (t.isEmpty()) return 0
        val dp = Array(t.size + 1) { IntArray(s.size + 1) }
        for (i in 1..t.size) for (j in 1..s.size) {
            dp[i][j] = if (t[i - 1] == s[j - 1]) dp[i - 1][j - 1] + 1 else maxOf(dp[i - 1][j], dp[i][j - 1])
        }
        val matched = dp[t.size][s.size]
        // 군더더기 단어가 많으면 약간 감점
        val extra = (s.size - matched).coerceAtLeast(0)
        val raw = matched * 100.0 / t.size - extra * 5
        return raw.toInt().coerceIn(0, 100)
    }

    /** 목표 문장 단어별로 맞게 말했는지 표시 */
    fun wordHits(target: String, spoken: String): List<Pair<String, Boolean>> {
        val spokenSet = normalize(spoken).toSet()
        return target.split(Regex("\\s+")).filter { it.isNotBlank() }.map { w ->
            val n = normalize(w).firstOrNull()
            w to (n == null || n in spokenSet)
        }
    }
}

/** 실제 대화에서 사용한 고유 단어 수 기반 레벨 */
object Levels {
    data class Level(
        val no: Int, val name: String, val need: Int, val emoji: String, val prompt: String,
        val cefr: String, val canDo: String,
    )

    val all = listOf(
        Level(1, "첫 마디", 0, "🌱", "absolute beginner (Pre-A1/A1): use very short, simple sentences and the most common words",
            "Pre-A1", "인사하고, 이름과 나이를 말할 수 있어요"),
        Level(2, "짧은 대답", 100, "🌿", "beginner (A1): short simple sentences, everyday words",
            "A1", "좋아하는 것, 가족, 물건을 짧게 말할 수 있어요"),
        Level(3, "문장으로 말하기", 300, "🌳", "elementary (A2): simple sentences, common phrasal verbs, slow pace",
            "A2", "주문하기, 길 묻기처럼 익숙한 상황에서 대화할 수 있어요"),
        Level(4, "실수하며 성장", 600, "🍀", "intermediate (B1): natural everyday English, some idioms explained by context",
            "B1", "경험과 계획을 이야기하고, 여행 중 대부분의 상황을 해결할 수 있어요"),
        Level(5, "자연스러운 대화", 1200, "🌻", "upper-intermediate (B2): natural native-like English with varied vocabulary",
            "B2", "원어민과 자연스럽게 대화하고 의견을 설명할 수 있어요"),
        Level(6, "유창한 표현가", 2500, "🏆", "advanced (C1): fully natural, nuanced English",
            "C1", "업무·토론에서 미묘한 차이까지 표현할 수 있어요"),
    )

    fun of(usedWords: Int): Level = all.last { usedWords >= it.need }
    fun next(level: Level): Level? = all.getOrNull(level.no)
    fun progress(usedWords: Int): Float {
        val cur = of(usedWords)
        val nxt = next(cur) ?: return 1f
        return ((usedWords - cur.need).toFloat() / (nxt.need - cur.need)).coerceIn(0f, 1f)
    }

    private val stop = setOf(
        "i", "a", "an", "the", "is", "am", "are", "was", "were", "be", "to", "of", "and", "or", "in", "on", "at",
        "it", "you", "me", "my", "we", "he", "she", "they", "do", "does", "did", "so", "um", "uh", "oh", "ok", "okay",
    )

    /** 레벨 산정용 단어 추출 (기능어 제외) */
    fun contentWords(text: String): Set<String> =
        SpeechScore.normalize(text).map { it.trim('\'') }
            .filter { it.length >= 2 && it !in stop && it.all { c -> c.isLetter() || c == '\'' } }
            .toSet()
}

/** 같은 학습 중에 틀린 카드를 몇 장 뒤에 다시 보여주는 순서 정하기 */
object Requeue {
    /** 현재 위치 index 의 카드를 틀렸을 때, gap 장 뒤(또는 맨 끝)에 다시 넣은 새 목록 */
    fun <T> again(list: List<T>, index: Int, gap: Int = 3, maxRepeats: Int = 2, countOf: (T) -> Int): List<T> {
        val item = list[index]
        if (countOf(item) >= maxRepeats) return list
        val pos = (index + 1 + gap).coerceAtMost(list.size)
        return list.toMutableList().also { it.add(pos, item) }
    }
}

object Streak {
    /** 활동한 날짜 목록(yyyy-MM-dd)으로 연속 학습일 계산. 오늘 안 했어도 어제까지 이어졌으면 유지 */
    fun count(activeDates: Set<String>, today: LocalDate = LocalDate.now()): Int {
        var d = if (today.toString() in activeDates) today else today.minusDays(1)
        var n = 0
        while (d.toString() in activeDates) { n++; d = d.minusDays(1) }
        return n
    }
}

object Sentences {
    /** 기사 본문을 문장 단위로 나눈다 (약어 일부 보호) */
    fun split(text: String): List<String> {
        val protected = text
            .replace(Regex("\\s+"), " ")
            .replace(Regex("\\b(Mr|Mrs|Ms|Dr|Prof|St|Jr|Sr|U\\.S|U\\.K|e\\.g|i\\.e|vs|No)\\."), "$1§")
        return protected.split(Regex("(?<=[.!?][\"”’)]?)\\s+(?=[\"“‘(]?[A-Z0-9])"))
            .map { it.replace("§", ".").trim() }
            .filter { it.length > 1 }
    }
}
