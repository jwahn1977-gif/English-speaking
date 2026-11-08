package com.jwahn.englishspeaking.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ChapterEntity::class, WordEntity::class,
        PatternEntity::class, PatternExampleEntity::class,
        ScenarioEntity::class, ConversationEntity::class, MessageEntity::class,
        UsedWordEntity::class, DailyStatEntity::class,
        NewsFeedEntity::class, NewsLessonEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vocab(): VocabDao
    abstract fun patterns(): PatternDao
    abstract fun tutor(): TutorDao
    abstract fun stats(): StatDao
    abstract fun news(): NewsDao

    companion object {
        /** v1 → v2: 왕초보 과정(챕터 레벨·이모지), 단어 이모지 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chapter ADD COLUMN level INTEGER NOT NULL DEFAULT 2")
                db.execSQL("ALTER TABLE chapter ADD COLUMN emoji TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE word ADD COLUMN emoji TEXT NOT NULL DEFAULT ''")
            }
        }

        /** 가족 프로필마다 별도 DB 파일. 기본 프로필은 기존 파일을 그대로 사용 */
        fun fileName(profileId: String) =
            if (profileId == "default") "english_speaking.db" else "english_speaking_$profileId.db"

        fun create(context: Context, profileId: String = "default"): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, fileName(profileId))
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
