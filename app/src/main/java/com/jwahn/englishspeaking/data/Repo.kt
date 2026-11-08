package com.jwahn.englishspeaking.data

import androidx.room.withTransaction
import com.jwahn.englishspeaking.data.db.AppDatabase
import com.jwahn.englishspeaking.data.db.BeginnerSeed
import com.jwahn.englishspeaking.data.db.ChapterEntity
import com.jwahn.englishspeaking.data.db.DailyStatEntity
import com.jwahn.englishspeaking.data.db.NewsFeedEntity
import com.jwahn.englishspeaking.data.db.PatternEntity
import com.jwahn.englishspeaking.data.db.PatternExampleEntity
import com.jwahn.englishspeaking.data.db.ScenarioEntity
import com.jwahn.englishspeaking.data.db.Seed
import com.jwahn.englishspeaking.data.db.UsedWordEntity
import com.jwahn.englishspeaking.data.db.WordEntity
import com.jwahn.englishspeaking.domain.Levels
import com.jwahn.englishspeaking.domain.objs
import com.jwahn.englishspeaking.domain.str
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.time.LocalDate

data class PatternDraft(
    val pattern: String,
    val meaningKo: String,
    val examples: List<Pair<String, String>>,
    val category: String = "일반",
) {
    companion object {
        /** {"pattern","meaning_ko","example_en","example_ko"} 또는 {"examples":[{"en","ko"}]} 형식 모두 지원 */
        fun from(o: JsonObject, defaultCategory: String = "일반"): PatternDraft? {
            val p = o.str("pattern").ifBlank { o.str("template") }
            if (p.isBlank()) return null
            val ex = mutableListOf<Pair<String, String>>()
            if (o.str("example_en").isNotBlank()) ex += o.str("example_en") to o.str("example_ko")
            o.objs("examples").forEach { e -> if (e.str("en").isNotBlank()) ex += e.str("en") to e.str("ko") }
            return PatternDraft(p, o.str("meaning_ko"), ex, o.str("category").ifBlank { defaultCategory })
        }
    }
}

data class WordDraft(
    val word: String,
    val ipa: String,
    val meaningKo: String,
    val exampleEn: String,
    val exampleKo: String,
) {
    companion object {
        fun from(o: JsonObject): WordDraft? {
            val w = o.str("word")
            if (w.isBlank()) return null
            return WordDraft(w, o.str("ipa").trim('/'), o.str("meaning_ko"), o.str("example_en"), o.str("example_ko"))
        }
    }
}

@Serializable
data class Backup(
    val version: Int = 1,
    val chapters: List<BChapter> = emptyList(),
    val patterns: List<BPattern> = emptyList(),
    val scenarios: List<BScenario> = emptyList(),
    val feeds: List<BFeed> = emptyList(),
)

@Serializable
data class BChapter(val title: String, val description: String, val source: String, val words: List<BWord>)

@Serializable
data class BWord(
    val text: String, val ipa: String, val meaningKo: String, val exampleEn: String, val exampleKo: String,
    val bookmarked: Boolean = false, val box: Int = 0, val nextDue: Long = 0,
)

@Serializable
data class BPattern(
    val template: String, val meaningKo: String, val category: String, val source: String,
    val box: Int = 0, val nextDue: Long = 0, val examples: List<List<String>> = emptyList(),
)

@Serializable
data class BScenario(
    val category: String, val title: String, val emoji: String, val persona: String, val goal: String, val missions: String,
)

@Serializable
data class BFeed(val name: String, val url: String)

class Repo(val db: AppDatabase) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    suspend fun seedIfNeeded() {
        if (db.vocab().chapterCount() == 0) {
            db.withTransaction {
                Seed.chapters().forEach { (c, words) ->
                    val id = db.vocab().insertChapter(c)
                    db.vocab().insertWords(words.map { it.copy(chapterId = id) })
                }
                Seed.patterns.forEach { p ->
                    savePattern(PatternDraft(p.template, p.meaning, p.examples, p.category), source = "builtin")
                }
            }
        }
        // 왕초보 과정 (v0.3에 추가, 기존 사용자에게도 넣는다)
        if (db.vocab().chapterCountBySource("beginner") == 0) {
            db.withTransaction {
                BeginnerSeed.all().forEach { (c, words) ->
                    val id = db.vocab().insertChapter(c)
                    db.vocab().insertWords(words.map { it.copy(chapterId = id) })
                }
            }
        }
        if (db.tutor().scenarioCount() == 0) Seed.scenarios.forEach { db.tutor().insertScenario(it) }
        if (db.news().feedCount() == 0) Seed.feeds.forEach { db.news().insertFeed(it) }
    }

    // ---------- 패턴 ----------

    private fun normalizeTemplate(t: String) = t.trim()
        .replace(Regex("\\s+"), " ")
        .replace("…", "~").replace("...", "~").replace("～", "~")
        .removeSuffix(".").trim()

    /** 패턴 저장. 같은 패턴이 있으면 예문만 추가한다. 반환: (패턴 id, 새로 만들었는지) */
    suspend fun savePattern(d: PatternDraft, source: String): Pair<Long, Boolean> {
        val template = normalizeTemplate(d.pattern)
        val dao = db.patterns()
        val existing = dao.byTemplate(template)
        val id: Long
        val created: Boolean
        if (existing != null) {
            id = existing.id; created = false
        } else {
            id = dao.insert(
                PatternEntity(template = template, meaningKo = d.meaningKo, category = d.category.ifBlank { "일반" }, source = source)
            )
            created = true
        }
        d.examples.forEach { (en, ko) ->
            if (en.isNotBlank() && dao.exampleExists(id, en.trim()) == 0) {
                dao.insertExample(PatternExampleEntity(patternId = id, en = en.trim(), ko = ko.trim()))
            }
        }
        return id to created
    }

    // ---------- 단어 ----------

    private suspend fun chapterFor(title: String, source: String, desc: String): Long {
        val found = db.vocab().allChapters().firstOrNull { it.title == title }
        return found?.id ?: db.vocab().insertChapter(ChapterEntity(title = title, description = desc, source = source))
    }

    /** 대화·뉴스에서 나온 단어를 지정 챕터에 저장. 이미 있는 단어면 false */
    suspend fun saveWord(d: WordDraft, chapterTitle: String, source: String): Boolean {
        if (db.vocab().findWord(d.word) != null) return false
        val cid = chapterFor(chapterTitle, source, "학습 중 저장한 단어")
        db.vocab().insertWord(
            WordEntity(chapterId = cid, text = d.word, ipa = d.ipa, meaningKo = d.meaningKo, exampleEn = d.exampleEn, exampleKo = d.exampleKo)
        )
        return true
    }

    suspend fun createChapter(title: String, desc: String, source: String, words: List<WordDraft>): Long {
        return db.withTransaction {
            val id = db.vocab().insertChapter(ChapterEntity(title = title, description = desc, source = source))
            db.vocab().insertWords(words.map {
                WordEntity(chapterId = id, text = it.word, ipa = it.ipa, meaningKo = it.meaningKo, exampleEn = it.exampleEn, exampleKo = it.exampleKo)
            })
            id
        }
    }

    // ---------- 통계 ----------

    suspend fun recordActivity(reviews: Int = 0, sentences: Int = 0, listened: Int = 0) {
        val date = LocalDate.now().toString()
        val cur = db.stats().get(date) ?: DailyStatEntity(date)
        db.stats().put(cur.copy(reviews = cur.reviews + reviews, sentences = cur.sentences + sentences, listened = cur.listened + listened))
    }

    suspend fun addUsedWords(text: String) {
        val words = Levels.contentWords(text)
        if (words.isNotEmpty()) db.tutor().insertUsedWords(words.map { UsedWordEntity(it) })
    }

    // ---------- 백업 ----------

    suspend fun exportJson(): String {
        val v = db.vocab(); val p = db.patterns()
        val words = v.allWords().groupBy { it.chapterId }
        val examples = p.allExamples().groupBy { it.patternId }
        val backup = Backup(
            chapters = v.allChapters().map { c ->
                BChapter(c.title, c.description, c.source, words[c.id].orEmpty().map {
                    BWord(it.text, it.ipa, it.meaningKo, it.exampleEn, it.exampleKo, it.bookmarked, it.box, it.nextDue)
                })
            },
            patterns = p.allOnce().map { pt ->
                BPattern(pt.template, pt.meaningKo, pt.category, pt.source, pt.box, pt.nextDue,
                    examples[pt.id].orEmpty().map { listOf(it.en, it.ko) })
            },
            scenarios = db.tutor().customScenarios().map { BScenario(it.category, it.title, it.emoji, it.persona, it.goal, it.missions) },
            feeds = db.news().feedsOnce().map { BFeed(it.name, it.url) },
        )
        return json.encodeToString(Backup.serializer(), backup)
    }

    /** 가져오기: 기존 데이터에 합친다(같은 챕터 이름/패턴/피드는 중복 방지). 반환: 요약 문구 */
    suspend fun importJson(text: String): String {
        val b = json.decodeFromString(Backup.serializer(), text)
        var words = 0; var patterns = 0; var scenarios = 0; var feeds = 0
        db.withTransaction {
            val existingTitles = db.vocab().allChapters().associateBy { it.title }
            b.chapters.forEach { c ->
                val cid = existingTitles[c.title]?.id
                    ?: db.vocab().insertChapter(ChapterEntity(title = c.title, description = c.description, source = c.source))
                c.words.forEach { w ->
                    if (db.vocab().findWord(w.text) == null) {
                        db.vocab().insertWord(WordEntity(chapterId = cid, text = w.text, ipa = w.ipa, meaningKo = w.meaningKo,
                            exampleEn = w.exampleEn, exampleKo = w.exampleKo, bookmarked = w.bookmarked, box = w.box, nextDue = w.nextDue))
                        words++
                    }
                }
            }
            b.patterns.forEach { bp ->
                val (_, created) = savePattern(
                    PatternDraft(bp.template, bp.meaningKo, bp.examples.map { it.getOrElse(0) { "" } to it.getOrElse(1) { "" } }, bp.category),
                    bp.source,
                )
                if (created) patterns++
            }
            val titles = db.tutor().customScenarios().map { it.title }.toSet()
            b.scenarios.filter { it.title !in titles }.forEach {
                db.tutor().insertScenario(ScenarioEntity(category = it.category, title = it.title, emoji = it.emoji,
                    persona = it.persona, goal = it.goal, missions = it.missions, builtin = false))
                scenarios++
            }
            val urls = db.news().feedsOnce().map { it.url }.toSet()
            b.feeds.filter { it.url !in urls }.forEach { db.news().insertFeed(NewsFeedEntity(name = it.name, url = it.url)); feeds++ }
        }
        return "단어 ${words}개, 패턴 ${patterns}개, 시나리오 ${scenarios}개, 뉴스 피드 ${feeds}개를 가져왔어요."
    }
}
