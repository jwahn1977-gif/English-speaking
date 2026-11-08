package com.jwahn.englishspeaking.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChapterEntity::class, WordEntity::class,
        PatternEntity::class, PatternExampleEntity::class,
        ScenarioEntity::class, ConversationEntity::class, MessageEntity::class,
        UsedWordEntity::class, DailyStatEntity::class,
        NewsFeedEntity::class, NewsLessonEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vocab(): VocabDao
    abstract fun patterns(): PatternDao
    abstract fun tutor(): TutorDao
    abstract fun stats(): StatDao
    abstract fun news(): NewsDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "english_speaking.db")
                .build()
    }
}
