package com.jwahn.englishspeaking.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class ChapterStat(
    val id: Long,
    val title: String,
    val description: String,
    val source: String,
    val total: Int,
    val studied: Int,
    val memorized: Int,
    val boxSum: Int,
    val best: Int,
    val level: Int,
    val emoji: String,
) {
    /** 암기율: 상자 3 이상 비율 */
    val memorizeRate: Int get() = if (total == 0) 0 else memorized * 100 / total
    /** 암기강도: 평균 상자 / 5 */
    val strength: Int get() = if (total == 0) 0 else boxSum * 100 / (total * 5)
    /** 진도율: 한 번 이상 학습한 비율 */
    val progress: Int get() = if (total == 0) 0 else studied * 100 / total
}

@Dao
interface VocabDao {
    @Insert
    suspend fun insertChapter(c: ChapterEntity): Long

    @Update
    suspend fun updateChapter(c: ChapterEntity)

    @Query("DELETE FROM chapter WHERE id = :id")
    suspend fun deleteChapter(id: Long)

    @Query("DELETE FROM word WHERE chapterId = :chapterId")
    suspend fun deleteWordsOfChapter(chapterId: Long)

    @Query("SELECT * FROM chapter WHERE id = :id")
    suspend fun chapter(id: Long): ChapterEntity?

    @Query("SELECT * FROM chapter ORDER BY id")
    suspend fun allChapters(): List<ChapterEntity>

    @Query("SELECT COUNT(*) FROM chapter")
    suspend fun chapterCount(): Int

    @Query(
        """
        SELECT c.id, c.title, c.description, c.source,
               COUNT(w.id) AS total,
               COALESCE(SUM(CASE WHEN w.box > 0 THEN 1 ELSE 0 END), 0) AS studied,
               COALESCE(SUM(CASE WHEN w.box >= 3 THEN 1 ELSE 0 END), 0) AS memorized,
               COALESCE(SUM(w.box), 0) AS boxSum,
               COALESCE(MAX(w.bestScore), 0) AS best,
               c.level AS level, c.emoji AS emoji
        FROM chapter c LEFT JOIN word w ON w.chapterId = c.id
        GROUP BY c.id ORDER BY c.level, c.id
        """
    )
    fun chapterStats(): Flow<List<ChapterStat>>

    @Insert
    suspend fun insertWords(words: List<WordEntity>): List<Long>

    @Insert
    suspend fun insertWord(word: WordEntity): Long

    @Update
    suspend fun updateWord(word: WordEntity)

    @Delete
    suspend fun deleteWord(word: WordEntity)

    @Query("SELECT * FROM word WHERE id = :id")
    suspend fun word(id: Long): WordEntity?

    @Query("SELECT * FROM word WHERE chapterId = :chapterId ORDER BY id")
    fun wordsOfChapter(chapterId: Long): Flow<List<WordEntity>>

    @Query("SELECT * FROM word WHERE chapterId = :chapterId ORDER BY id")
    suspend fun wordsOfChapterOnce(chapterId: Long): List<WordEntity>

    @Query("SELECT COUNT(*) FROM chapter WHERE source = :source")
    suspend fun chapterCountBySource(source: String): Int

    /** 아직 학습하지 않은 단어가 남은 첫 챕터 (레벨 이하) */
    @Query(
        """
        SELECT c.* FROM chapter c WHERE c.level <= :maxLevel AND EXISTS
          (SELECT 1 FROM word w WHERE w.chapterId = c.id AND w.box = 0)
        ORDER BY c.level, c.id LIMIT 1
        """
    )
    suspend fun nextChapterToLearn(maxLevel: Int): ChapterEntity?

    @Query("SELECT * FROM word WHERE chapterId = :chapterId AND box = 0 ORDER BY id LIMIT :limit")
    suspend fun newWords(chapterId: Long, limit: Int): List<WordEntity>

    @Query("SELECT * FROM word WHERE bookmarked = 1 ORDER BY id")
    suspend fun bookmarkedWords(): List<WordEntity>

    @Query("SELECT * FROM word WHERE box > 0 AND nextDue <= :now ORDER BY nextDue LIMIT :limit")
    suspend fun dueWords(now: Long, limit: Int = 200): List<WordEntity>

    @Query("SELECT COUNT(*) FROM word WHERE box > 0 AND nextDue <= :now")
    fun dueWordCount(now: Long): Flow<Int>

    @Query("SELECT * FROM word ORDER BY RANDOM() LIMIT :n")
    suspend fun randomWords(n: Int): List<WordEntity>

    @Query("SELECT * FROM word WHERE LOWER(text) = LOWER(:text) LIMIT 1")
    suspend fun findWord(text: String): WordEntity?

    @Query("SELECT * FROM word ORDER BY id")
    suspend fun allWords(): List<WordEntity>

    @Query("SELECT COUNT(*) FROM word")
    fun wordCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM word WHERE box >= 3")
    fun memorizedCount(): Flow<Int>
}

@Dao
interface PatternDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(p: PatternEntity): Long

    @Update
    suspend fun update(p: PatternEntity)

    @Query("DELETE FROM pattern WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM pattern_example WHERE patternId = :patternId")
    suspend fun deleteExamples(patternId: Long)

    @Query("SELECT * FROM pattern WHERE template = :template LIMIT 1")
    suspend fun byTemplate(template: String): PatternEntity?

    @Query("SELECT * FROM pattern WHERE id = :id")
    suspend fun byId(id: Long): PatternEntity?

    @Query("SELECT * FROM pattern ORDER BY createdAt DESC")
    fun all(): Flow<List<PatternEntity>>

    @Query("SELECT * FROM pattern ORDER BY id")
    suspend fun allOnce(): List<PatternEntity>

    @Query("SELECT COUNT(*) FROM pattern")
    fun count(): Flow<Int>

    @Insert
    suspend fun insertExample(e: PatternExampleEntity): Long

    @Query("SELECT * FROM pattern_example WHERE patternId = :patternId ORDER BY id")
    fun examples(patternId: Long): Flow<List<PatternExampleEntity>>

    @Query("SELECT * FROM pattern_example WHERE patternId = :patternId ORDER BY id")
    suspend fun examplesOnce(patternId: Long): List<PatternExampleEntity>

    @Query("SELECT * FROM pattern_example ORDER BY id")
    suspend fun allExamples(): List<PatternExampleEntity>

    @Query("SELECT COUNT(*) FROM pattern_example WHERE patternId = :patternId AND LOWER(en) = LOWER(:en)")
    suspend fun exampleExists(patternId: Long, en: String): Int

    @Query("SELECT * FROM pattern WHERE box > 0 AND nextDue <= :now ORDER BY nextDue LIMIT :limit")
    suspend fun due(now: Long, limit: Int = 100): List<PatternEntity>

    @Query("SELECT * FROM pattern WHERE box = 0 ORDER BY createdAt LIMIT :limit")
    suspend fun unstudied(limit: Int): List<PatternEntity>

    @Query("SELECT COUNT(*) FROM pattern WHERE box > 0 AND nextDue <= :now")
    fun dueCount(now: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM pattern WHERE box = 0")
    fun unstudiedCount(): Flow<Int>
}

@Dao
interface TutorDao {
    @Insert
    suspend fun insertScenario(s: ScenarioEntity): Long

    @Query("DELETE FROM scenario WHERE id = :id")
    suspend fun deleteScenario(id: Long)

    @Query("SELECT * FROM scenario ORDER BY builtin DESC, id")
    fun scenarios(): Flow<List<ScenarioEntity>>

    @Query("SELECT * FROM scenario ORDER BY id")
    suspend fun allScenarios(): List<ScenarioEntity>

    @Query("SELECT * FROM scenario WHERE builtin = 0 ORDER BY id")
    suspend fun customScenarios(): List<ScenarioEntity>

    @Query("SELECT * FROM scenario WHERE id = :id")
    suspend fun scenario(id: Long): ScenarioEntity?

    @Query("SELECT COUNT(*) FROM scenario")
    suspend fun scenarioCount(): Int

    @Insert
    suspend fun insertConversation(c: ConversationEntity): Long

    @Update
    suspend fun updateConversation(c: ConversationEntity)

    @Query("SELECT * FROM conversation WHERE id = :id")
    suspend fun conversation(id: Long): ConversationEntity?

    @Query("SELECT * FROM conversation ORDER BY startedAt DESC LIMIT :limit")
    fun recentConversations(limit: Int = 30): Flow<List<ConversationEntity>>

    @Query("DELETE FROM conversation WHERE id = :id")
    suspend fun deleteConversation(id: Long)

    @Query("DELETE FROM message WHERE conversationId = :id")
    suspend fun deleteMessages(id: Long)

    @Insert
    suspend fun insertMessage(m: MessageEntity): Long

    @Query("SELECT * FROM message WHERE conversationId = :conversationId ORDER BY id")
    suspend fun messages(conversationId: Long): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUsedWords(words: List<UsedWordEntity>)

    @Query("SELECT COUNT(*) FROM used_word")
    fun usedWordCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM used_word")
    suspend fun usedWordCountOnce(): Int
}

@Dao
interface StatDao {
    @Query("SELECT * FROM daily_stat WHERE date = :date")
    suspend fun get(date: String): DailyStatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(s: DailyStatEntity)

    @Query("SELECT * FROM daily_stat ORDER BY date DESC LIMIT 400")
    fun recent(): Flow<List<DailyStatEntity>>
}

@Dao
interface NewsDao {
    @Insert
    suspend fun insertFeed(f: NewsFeedEntity): Long

    @Update
    suspend fun updateFeed(f: NewsFeedEntity)

    @Query("DELETE FROM news_feed WHERE id = :id")
    suspend fun deleteFeed(id: Long)

    @Query("SELECT * FROM news_feed ORDER BY id")
    fun feeds(): Flow<List<NewsFeedEntity>>

    @Query("SELECT * FROM news_feed ORDER BY id")
    suspend fun feedsOnce(): List<NewsFeedEntity>

    @Query("SELECT COUNT(*) FROM news_feed")
    suspend fun feedCount(): Int

    @Insert
    suspend fun insertLesson(l: NewsLessonEntity): Long

    @Query("UPDATE news_lesson SET lastPosition = :pos WHERE id = :id")
    suspend fun updateLessonPosition(id: Long, pos: Int)

    @Query("SELECT * FROM news_lesson WHERE id = :id")
    suspend fun lesson(id: Long): NewsLessonEntity?

    @Query("SELECT * FROM news_lesson WHERE url = :url AND mode = :mode ORDER BY id DESC LIMIT 1")
    suspend fun lessonFor(url: String, mode: String): NewsLessonEntity?

    @Query("SELECT * FROM news_lesson ORDER BY createdAt DESC LIMIT 50")
    fun recentLessons(): Flow<List<NewsLessonEntity>>

    @Query("DELETE FROM news_lesson WHERE id = :id")
    suspend fun deleteLesson(id: Long)
}
