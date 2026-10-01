package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        GameEntity::class,
        PlayerEntity::class,
        TargetEntity::class,
        PlayerNoteEntity::class,
        VoteEntity::class,
        ManualSuspicionEntity::class,
        AlgorithmWeightEntity::class,
        SecretNoteEntity::class,
        AiSettingsEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE games ADD COLUMN maxUnlockedStageIndex INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS secret_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        gameId INTEGER NOT NULL,
                        playerId INTEGER NOT NULL,
                        stageIndex INTEGER NOT NULL,
                        content TEXT NOT NULL,
                        misdirectionSuggestion TEXT NOT NULL DEFAULT '',
                        timestamp INTEGER NOT NULL,
                        FOREIGN KEY(gameId) REFERENCES games(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_secret_notes_gameId_playerId ON secret_notes(gameId, playerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_secret_notes_stageIndex ON secret_notes(stageIndex)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE games ADD COLUMN citizenCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE games ADD COLUMN mafiaCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE games ADD COLUMN independentCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE games ADD COLUMN ownerPlayerId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE games ADD COLUMN ownerRole TEXT NOT NULL DEFAULT 'CITIZEN'")
                db.execSQL("ALTER TABLE players ADD COLUMN isOwner INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE players ADD COLUMN knownRole TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ai_settings (
                        id INTEGER NOT NULL,
                        baseUrl TEXT NOT NULL,
                        apiKey TEXT NOT NULL,
                        model TEXT NOT NULL,
                        temperature REAL NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mafia_analyzer.db"
                )
                    // NOTE: No destructive fallback on purpose — every schema change must ship
                    // with an explicit migration so user data is never silently wiped.
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
